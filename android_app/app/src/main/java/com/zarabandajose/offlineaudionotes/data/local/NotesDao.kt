package com.zarabandajose.offlineaudionotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for notes table.
 * Provides methods for CRUD operations and search.
 */
@Dao
interface NotesDao {

    /**
     * Get all notes ordered by creation date (newest first).
     */
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    /**
     * Get a single note by ID.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    /**
     * Get a single note by ID as Flow for reactive updates.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdFlow(id: Long): Flow<NoteEntity?>

    /**
     * Insert a new note and return the generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    /**
     * Update an existing note.
     */
    @Update
    suspend fun updateNote(note: NoteEntity)

    /**
     * Delete a note by ID.
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    /**
     * Search notes by title or transcript (case-insensitive).
     * Returns notes where either title or transcript contains the query.
     */
    @Query("""
        SELECT * FROM notes 
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(transcript) LIKE '%' || LOWER(:query) || '%'
        ORDER BY createdAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    /**
     * Get the count of all notes.
     */
    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNotesCount(): Int

    /**
     * Update transcription status for a note.
     */
    @Query("UPDATE notes SET transcriptionStatus = :status, lastTranscriptionError = :error WHERE id = :noteId")
    suspend fun updateTranscriptionStatus(noteId: Long, status: String, error: String?)

    /**
     * Update transcript and status for a note after successful transcription.
     */
    @Query("UPDATE notes SET transcript = :transcript, title = COALESCE(title, :autoTitle), transcriptionStatus = :status, lastTranscriptionError = NULL WHERE id = :noteId")
    suspend fun updateTranscriptAndStatus(noteId: Long, transcript: String, autoTitle: String?, status: String)
}
