package com.zarabandajose.offlineaudionotes.domain.model

/**
 * Represents the transcription state of a note.
 */
enum class TranscriptionStatus {
    /** No transcription has been attempted yet */
    PENDING,
    /** Transcription is currently running in the background */
    IN_PROGRESS,
    /** Transcription completed successfully */
    COMPLETED,
    /** Transcription failed due to an error */
    FAILED,
    /** Transcription was cancelled by the user */
    CANCELLED
}
