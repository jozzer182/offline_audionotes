package com.zarabandajose.offlineaudionotes.audio

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

/**
 * Audio preprocessing utility for improving transcription quality.
 * 
 * This class analyzes audio files and normalizes the volume if the recording
 * is too quiet, which can improve whisper.cpp transcription accuracy.
 * 
 * The preprocessing:
 * 1. Reads the WAV file and calculates peak amplitude
 * 2. If peak is below threshold (50% of max), calculates normalization gain
 * 3. Creates a normalized copy of the audio with boosted volume
 * 4. Returns the path to the normalized file (or original if no normalization needed)
 */
object AudioPreprocessor {
    
    private const val TAG = "AudioPreprocessor"
    
    // 16-bit audio max value
    private const val MAX_16BIT = 32767
    
    // Target peak after normalization (90% of max to leave headroom)
    private const val TARGET_PEAK = (MAX_16BIT * 0.9).toInt()
    
    // Threshold below which we normalize (50% of max)
    private const val NORMALIZE_THRESHOLD = (MAX_16BIT * 0.5).toInt()
    
    // Minimum gain to apply (don't bother if gain is less than 1.2x)
    private const val MIN_GAIN = 1.2f
    
    // Maximum gain to prevent excessive amplification of noise
    private const val MAX_GAIN = 10.0f
    
    /**
     * Analyzes audio file and normalizes it if the volume is too low.
     * 
     * @param audioPath Path to the original WAV audio file
     * @return Path to use for transcription (normalized file or original)
     */
    fun normalizeIfNeeded(audioPath: String): String {
        val audioFile = File(audioPath)
        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file does not exist: $audioPath")
            return audioPath
        }
        
        try {
            // Read WAV file and analyze
            val wavData = readWavFile(audioPath) ?: return audioPath
            
            val peakAmplitude = findPeakAmplitude(wavData.samples)
            Log.d(TAG, "Peak amplitude: $peakAmplitude (threshold: $NORMALIZE_THRESHOLD)")
            
            if (peakAmplitude >= NORMALIZE_THRESHOLD) {
                Log.d(TAG, "Audio volume is sufficient, no normalization needed")
                return audioPath
            }
            
            if (peakAmplitude < 100) {
                Log.w(TAG, "Audio appears to be nearly silent, skipping normalization")
                return audioPath
            }
            
            // Calculate gain needed to reach target
            val gain = TARGET_PEAK.toFloat() / peakAmplitude.toFloat()
            val clampedGain = gain.coerceIn(MIN_GAIN, MAX_GAIN)
            
            if (clampedGain < MIN_GAIN) {
                Log.d(TAG, "Gain too small ($gain), skipping normalization")
                return audioPath
            }
            
            Log.d(TAG, "Applying normalization with gain: $clampedGain")
            
            // Apply gain
            val normalizedSamples = applyGain(wavData.samples, clampedGain)
            
            // Write normalized file
            val normalizedPath = audioPath.replace(".wav", "_normalized.wav")
            writeWavFile(normalizedPath, normalizedSamples, wavData.sampleRate, wavData.numChannels)
            
            Log.d(TAG, "Normalized audio saved to: $normalizedPath")
            return normalizedPath
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during audio normalization", e)
            return audioPath // Return original on error
        }
    }
    
    /**
     * Reads a WAV file and extracts the audio samples.
     */
    private fun readWavFile(path: String): WavData? {
        val file = RandomAccessFile(path, "r")
        
        try {
            // Read RIFF header
            val riff = ByteArray(4)
            file.read(riff)
            if (String(riff) != "RIFF") {
                Log.e(TAG, "Not a valid RIFF file")
                return null
            }
            
            file.skipBytes(4) // Skip file size
            
            val wave = ByteArray(4)
            file.read(wave)
            if (String(wave) != "WAVE") {
                Log.e(TAG, "Not a valid WAVE file")
                return null
            }
            
            // Read fmt chunk
            val fmt = ByteArray(4)
            file.read(fmt)
            if (String(fmt) != "fmt ") {
                Log.e(TAG, "Missing fmt chunk")
                return null
            }
            
            val fmtSize = readIntLE(file)
            val audioFormat = readShortLE(file)
            val numChannels = readShortLE(file)
            val sampleRate = readIntLE(file)
            file.skipBytes(4) // ByteRate
            file.skipBytes(2) // BlockAlign
            val bitsPerSample = readShortLE(file)
            
            // Skip any extra fmt bytes
            if (fmtSize > 16) {
                file.skipBytes(fmtSize - 16)
            }
            
            if (audioFormat != 1.toShort()) {
                Log.e(TAG, "Unsupported audio format: $audioFormat (only PCM supported)")
                return null
            }
            
            if (bitsPerSample != 16.toShort()) {
                Log.e(TAG, "Unsupported bit depth: $bitsPerSample (only 16-bit supported)")
                return null
            }
            
            // Find data chunk
            while (file.filePointer < file.length()) {
                val chunkId = ByteArray(4)
                file.read(chunkId)
                val chunkSize = readIntLE(file)
                
                if (String(chunkId) == "data") {
                    // Read audio samples
                    val numSamples = chunkSize / 2 // 16-bit = 2 bytes per sample
                    val samples = ShortArray(numSamples)
                    
                    val buffer = ByteArray(chunkSize)
                    file.read(buffer)
                    
                    val byteBuffer = ByteBuffer.wrap(buffer)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    
                    for (i in 0 until numSamples) {
                        samples[i] = byteBuffer.short
                    }
                    
                    return WavData(samples, sampleRate, numChannels.toInt())
                } else {
                    // Skip unknown chunk
                    file.skipBytes(chunkSize)
                }
            }
            
            Log.e(TAG, "No data chunk found in WAV file")
            return null
            
        } finally {
            file.close()
        }
    }
    
    /**
     * Finds the peak (maximum absolute) amplitude in the samples.
     */
    private fun findPeakAmplitude(samples: ShortArray): Int {
        var peak = 0
        for (sample in samples) {
            val absValue = abs(sample.toInt())
            if (absValue > peak) {
                peak = absValue
            }
        }
        return peak
    }
    
    /**
     * Applies gain to samples with clipping prevention.
     */
    private fun applyGain(samples: ShortArray, gain: Float): ShortArray {
        val result = ShortArray(samples.size)
        for (i in samples.indices) {
            val amplified = (samples[i] * gain).toInt()
            // Clamp to prevent clipping
            result[i] = amplified.coerceIn(-MAX_16BIT, MAX_16BIT).toShort()
        }
        return result
    }
    
    /**
     * Writes a WAV file with the given samples.
     */
    private fun writeWavFile(path: String, samples: ShortArray, sampleRate: Int, numChannels: Int) {
        val file = RandomAccessFile(path, "rw")
        val dataSize = samples.size * 2
        val totalSize = dataSize + 36
        
        try {
            // RIFF header
            file.writeBytes("RIFF")
            file.write(intToByteArrayLE(totalSize))
            file.writeBytes("WAVE")
            
            // fmt chunk
            file.writeBytes("fmt ")
            file.write(intToByteArrayLE(16)) // Subchunk1Size
            file.write(shortToByteArrayLE(1)) // AudioFormat (PCM)
            file.write(shortToByteArrayLE(numChannels.toShort()))
            file.write(intToByteArrayLE(sampleRate))
            file.write(intToByteArrayLE(sampleRate * numChannels * 16 / 8)) // ByteRate
            file.write(shortToByteArrayLE((numChannels * 16 / 8).toShort())) // BlockAlign
            file.write(shortToByteArrayLE(16)) // BitsPerSample
            
            // data chunk
            file.writeBytes("data")
            file.write(intToByteArrayLE(dataSize))
            
            // Write samples
            val byteBuffer = ByteBuffer.allocate(dataSize)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            for (sample in samples) {
                byteBuffer.putShort(sample)
            }
            file.write(byteBuffer.array())
            
        } finally {
            file.close()
        }
    }
    
    private fun readIntLE(file: RandomAccessFile): Int {
        val bytes = ByteArray(4)
        file.read(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }
    
    private fun readShortLE(file: RandomAccessFile): Short {
        val bytes = ByteArray(2)
        file.read(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short
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
     * Internal data class to hold WAV file data.
     */
    private data class WavData(
        val samples: ShortArray,
        val sampleRate: Int,
        val numChannels: Int
    )
}
