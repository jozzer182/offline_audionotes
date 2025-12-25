package com.zarabandajose.offlineaudionotes.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.zarabandajose.offlineaudionotes.R
import com.zarabandajose.offlineaudionotes.data.local.OfflineAudioNotesDatabase
import com.zarabandajose.offlineaudionotes.domain.model.TranscriptionStatus
import com.zarabandajose.offlineaudionotes.stt.WhisperBridge
import com.zarabandajose.offlineaudionotes.stt.WhisperModelManager
import com.zarabandajose.offlineaudionotes.ui.NoteDetailActivity
import com.zarabandajose.offlineaudionotes.audio.AudioPreprocessor
import java.io.File

/**
 * Background worker for transcribing audio notes using whisper.cpp.
 * Runs as a foreground worker with an ongoing notification.
 */
class TranscribeNoteWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "TranscribeNoteWorker"
        const val KEY_NOTE_ID = "note_id"
        const val KEY_AUDIO_PATH = "audio_path"
        const val KEY_LANGUAGE = "language"
        
        const val CHANNEL_ID = "transcription_channel"
        const val NOTIFICATION_ID_PROGRESS = 1001
        const val NOTIFICATION_ID_COMPLETE = 1002
        
        private const val MAX_TITLE_LENGTH = 50

        fun getUniqueWorkName(noteId: Long): String = "transcription_note_$noteId"

        /**
         * Create the notification channel for transcription notifications.
         * Should be called once at app startup.
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.transcription_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.transcription_channel_description)
                    setShowBadge(false)
                }
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private val notesDao by lazy {
        OfflineAudioNotesDatabase.getInstance(context).notesDao()
    }

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        val audioPath = inputData.getString(KEY_AUDIO_PATH) ?: ""
        val language = inputData.getString(KEY_LANGUAGE) ?: "en"

        if (noteId == -1L || audioPath.isBlank()) {
            Log.e(TAG, "Invalid input: noteId=$noteId, audioPath=$audioPath")
            return Result.failure()
        }

        Log.d(TAG, "Starting transcription for note $noteId")

        try {
            setForeground(createForegroundInfo())
            
            notesDao.updateTranscriptionStatus(
                noteId, 
                TranscriptionStatus.IN_PROGRESS.name, 
                null
            )

            if (!File(audioPath).exists()) {
                val error = context.getString(R.string.audio_file_missing)
                notesDao.updateTranscriptionStatus(noteId, TranscriptionStatus.FAILED.name, error)
                return Result.failure(workDataOf("error" to error))
            }

            val modelPath = WhisperModelManager.getModelPath(context)
            if (modelPath == null) {
                val error = context.getString(R.string.model_not_found)
                notesDao.updateTranscriptionStatus(noteId, TranscriptionStatus.FAILED.name, error)
                return Result.failure(workDataOf("error" to error))
            }

            // Preprocess audio for better transcription (normalize volume if needed)
            val processedAudioPath = AudioPreprocessor.normalizeIfNeeded(audioPath)

            val result = WhisperBridge.transcribeFile(modelPath, processedAudioPath, language)

            if (isStopped) {
                Log.d(TAG, "Worker was stopped/cancelled for note $noteId")
                notesDao.updateTranscriptionStatus(noteId, TranscriptionStatus.CANCELLED.name, null)
                return Result.failure()
            }

            if (WhisperBridge.isError(result)) {
                val errorMsg = WhisperBridge.getErrorMessage(result)
                Log.e(TAG, "Transcription failed for note $noteId: $errorMsg")
                notesDao.updateTranscriptionStatus(noteId, TranscriptionStatus.FAILED.name, errorMsg)
                return Result.failure(workDataOf("error" to errorMsg))
            }

            val autoTitle = generateTitleFromTranscript(result)
            notesDao.updateTranscriptAndStatus(
                noteId,
                result,
                autoTitle,
                TranscriptionStatus.COMPLETED.name
            )

            Log.d(TAG, "Transcription completed for note $noteId")
            showCompletionNotification(noteId)

            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Exception during transcription for note $noteId", e)
            val errorMsg = e.message ?: "Unknown error"
            notesDao.updateTranscriptionStatus(noteId, TranscriptionStatus.FAILED.name, errorMsg)
            return Result.failure(workDataOf("error" to errorMsg))
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val cancelIntent = androidx.work.WorkManager.getInstance(context)
            .createCancelPendingIntent(id)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.transcribing_notification_title))
            .setContentText(context.getString(R.string.transcribing_notification_text))
            .setSmallIcon(R.drawable.ic_transcribe)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .addAction(
                R.drawable.ic_stop,
                context.getString(R.string.cancel),
                cancelIntent
            )
            .build()

        // For Android 14+ (SDK 34+), we must specify foreground service type
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NOTIFICATION_ID_PROGRESS, 
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID_PROGRESS, notification)
        }
    }

    private fun showCompletionNotification(noteId: Long) {
        val intent = NoteDetailActivity.newIntent(context, noteId).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.transcription_complete_title))
            .setContentText(context.getString(R.string.transcription_complete_text))
            .setSmallIcon(R.drawable.ic_audio_note)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_COMPLETE + noteId.toInt(), notification)
    }

    private fun generateTitleFromTranscript(transcript: String): String? {
        if (transcript.isBlank()) return null
        
        val cleaned = transcript.trim()
        val firstLine = cleaned.lines().firstOrNull()?.trim() ?: cleaned
        
        return if (firstLine.length <= MAX_TITLE_LENGTH) {
            firstLine
        } else {
            firstLine.take(MAX_TITLE_LENGTH - 3) + "..."
        }
    }
}
