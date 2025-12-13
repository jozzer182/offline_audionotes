package com.zarabandajose.offlineaudionotes.domain.model

/**
 * Represents a single audio note with its transcription.
 */
data class Note(
    val id: Long,
    val createdAt: Long,
    val title: String?,
    val transcript: String,
    val audioPath: String,
    val durationMillis: Long?,
    val language: String,
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.PENDING,
    val lastTranscriptionError: String? = null
)
