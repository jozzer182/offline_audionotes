package com.zarabandajose.offlineaudionotes.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Manages audio recording using AudioRecord for WAV output.
 * 
 * Audio Format Choice (optimized for whisper.cpp):
 * - Format: WAV (RIFF) container with linear PCM
 * - Sample rate: 16kHz (whisper.cpp's preferred input rate)
 * - Channels: Mono (1 channel)
 * - Bit depth: 16-bit signed PCM
 * 
 * Why WAV format:
 * - whisper.cpp directly reads WAV files with 16-bit PCM audio
 * - No decoding step required - audio is already in the format whisper expects
 * - Simple and reliable format for speech recognition
 */
class AudioRecorderManager(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var currentFilePath: String? = null
    @Volatile private var isRecording = false
    private var recordingStartTime: Long = 0

    /**
     * Starts recording audio to a new WAV file.
     * @return The file path where audio will be saved, or null if recording failed to start.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(): String? {
        if (isRecording) {
            return currentFilePath
        }

        val audioFile = createAudioFile() ?: return null
        currentFilePath = audioFile.absolutePath

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return null
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                releaseRecorder()
                return null
            }

            audioRecord?.startRecording()
            isRecording = true
            recordingStartTime = System.currentTimeMillis()

            recordingThread = Thread {
                writeWavFile(currentFilePath!!, bufferSize)
            }.apply { start() }

            return currentFilePath
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            currentFilePath = null
            return null
        }
    }

    private fun writeWavFile(filePath: String, bufferSize: Int) {
        val buffer = ShortArray(bufferSize)
        val outputStream = FileOutputStream(filePath)

        try {
            // Write placeholder WAV header (will be updated when recording stops)
            val header = ByteArray(44)
            outputStream.write(header)

            var totalBytesWritten = 0

            while (isRecording) {
                val readCount = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (readCount > 0) {
                    val byteBuffer = ByteBuffer.allocate(readCount * 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until readCount) {
                        byteBuffer.putShort(buffer[i])
                    }
                    outputStream.write(byteBuffer.array())
                    totalBytesWritten += readCount * 2
                }
            }

            outputStream.close()

            // Update WAV header with correct file size
            updateWavHeader(filePath, totalBytesWritten)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                outputStream.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun updateWavHeader(filePath: String, dataSize: Int) {
        val file = RandomAccessFile(filePath, "rw")
        val totalSize = dataSize + 36

        file.seek(0)

        // RIFF header
        file.writeBytes("RIFF")
        file.write(intToByteArrayLE(totalSize))
        file.writeBytes("WAVE")

        // fmt chunk
        file.writeBytes("fmt ")
        file.write(intToByteArrayLE(16)) // Subchunk1Size (16 for PCM)
        file.write(shortToByteArrayLE(1)) // AudioFormat (1 = PCM)
        file.write(shortToByteArrayLE(NUM_CHANNELS.toShort())) // NumChannels
        file.write(intToByteArrayLE(SAMPLE_RATE)) // SampleRate
        file.write(intToByteArrayLE(SAMPLE_RATE * NUM_CHANNELS * BITS_PER_SAMPLE / 8)) // ByteRate
        file.write(shortToByteArrayLE((NUM_CHANNELS * BITS_PER_SAMPLE / 8).toShort())) // BlockAlign
        file.write(shortToByteArrayLE(BITS_PER_SAMPLE.toShort())) // BitsPerSample

        // data chunk
        file.writeBytes("data")
        file.write(intToByteArrayLE(dataSize))

        file.close()
    }

    private fun intToByteArrayLE(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArrayLE(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    /**
     * Stops the current recording.
     * @return A RecordingResult containing the file path and duration, or null if not recording.
     */
    fun stopRecording(): RecordingResult? {
        if (!isRecording) {
            return null
        }

        val filePath = currentFilePath
        val durationMillis = System.currentTimeMillis() - recordingStartTime

        isRecording = false

        try {
            recordingThread?.join(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        audioRecord = null
        recordingThread = null
        currentFilePath = null

        return if (filePath != null) {
            RecordingResult(filePath, durationMillis)
        } else {
            null
        }
    }

    /**
     * Releases the recorder resources. Call this when done with recording.
     */
    fun release() {
        if (isRecording) {
            stopRecording()
        }
        releaseRecorder()
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    fun getRecordingDurationMillis(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0
        }
    }

    private fun createAudioFile(): File? {
        val audioDir = getAudioDirectory()
        if (!audioDir.exists() && !audioDir.mkdirs()) {
            return null
        }
        val timestamp = System.currentTimeMillis()
        val fileName = "note_$timestamp.wav"
        return File(audioDir, fileName)
    }

    private fun getAudioDirectory(): File {
        return File(context.filesDir, AUDIO_DIRECTORY)
    }

    private fun releaseRecorder() {
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
        recordingThread = null
    }

    companion object {
        private const val AUDIO_DIRECTORY = "notes_audio"
        private const val SAMPLE_RATE = 16000
        private const val NUM_CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
    }
}

/**
 * Result of a completed recording.
 */
data class RecordingResult(
    val filePath: String,
    val durationMillis: Long
)
