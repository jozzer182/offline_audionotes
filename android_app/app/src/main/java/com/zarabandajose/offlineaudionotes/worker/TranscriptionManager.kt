package com.zarabandajose.offlineaudionotes.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manager for scheduling and controlling transcription work.
 * Provides a clean API for enqueueing, cancelling, and observing transcription jobs.
 */
object TranscriptionManager {

    /**
     * Enqueue a transcription job for a note.
     * Uses unique work to avoid duplicates and allow cancellation by note ID.
     */
    fun enqueueTranscription(
        context: Context,
        noteId: Long,
        audioPath: String,
        language: String
    ) {
        val inputData = workDataOf(
            TranscribeNoteWorker.KEY_NOTE_ID to noteId,
            TranscribeNoteWorker.KEY_AUDIO_PATH to audioPath,
            TranscribeNoteWorker.KEY_LANGUAGE to language
        )

        val workRequest = OneTimeWorkRequestBuilder<TranscribeNoteWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            TranscribeNoteWorker.getUniqueWorkName(noteId),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancel a transcription job for a note.
     */
    fun cancelTranscription(context: Context, noteId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(TranscribeNoteWorker.getUniqueWorkName(noteId))
    }

    /**
     * Observe the work state for a specific note's transcription.
     */
    fun observeTranscriptionState(context: Context, noteId: Long): Flow<WorkInfo.State?> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(TranscribeNoteWorker.getUniqueWorkName(noteId))
            .map { workInfos ->
                workInfos.firstOrNull()?.state
            }
    }

    /**
     * Check if a transcription is currently running for a note.
     */
    fun isTranscriptionRunning(context: Context, noteId: Long): Flow<Boolean> {
        return observeTranscriptionState(context, noteId).map { state ->
            state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
        }
    }
}
