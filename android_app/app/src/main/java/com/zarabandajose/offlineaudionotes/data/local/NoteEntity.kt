package com.zarabandajose.offlineaudionotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zarabandajose.offlineaudionotes.domain.model.Note
import com.zarabandajose.offlineaudionotes.domain.model.TranscriptionStatus

/**
 * Room entity representing a note in the local database.
 * Maps directly to the notes table.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    val title: String?,
    val transcript: String,
    val audioPath: String,
    val durationMillis: Long?,
    val language: String,
    val transcriptionStatus: String = TranscriptionStatus.PENDING.name,
    val lastTranscriptionError: String? = null
) {
    /**
     * Convert entity to domain model.
     */
    fun toDomainModel(): Note {
        return Note(
            id = id,
            createdAt = createdAt,
            title = title,
            transcript = transcript,
            audioPath = audioPath,
            durationMillis = durationMillis,
            language = language,
            transcriptionStatus = TranscriptionStatus.valueOf(transcriptionStatus),
            lastTranscriptionError = lastTranscriptionError
        )
    }

    companion object {
        /**
         * Create entity from domain model.
         */
        fun fromDomainModel(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
                createdAt = note.createdAt,
                title = note.title,
                transcript = note.transcript,
                audioPath = note.audioPath,
                durationMillis = note.durationMillis,
                language = note.language,
                transcriptionStatus = note.transcriptionStatus.name,
                lastTranscriptionError = note.lastTranscriptionError
            )
        }
    }
}
