package com.zarabandajose.offlineaudionotes.data.repository

import com.zarabandajose.offlineaudionotes.domain.model.Note

/**
 * Repository interface for managing notes.
 * This abstraction allows swapping implementations (in-memory, Room, etc.).
 */
interface NotesRepository {
    
    /**
     * Returns all notes, ordered by creation date (newest first).
     */
    suspend fun getAllNotes(): List<Note>
    
    /**
     * Returns a single note by its ID, or null if not found.
     */
    suspend fun getNoteById(id: Long): Note?
    
    /**
     * Saves a new note and returns the saved note with its assigned ID.
     */
    suspend fun saveNote(note: Note): Note
    
    /**
     * Updates an existing note.
     */
    suspend fun updateNote(note: Note)
    
    /**
     * Deletes a note by its ID.
     */
    suspend fun deleteNote(id: Long)
    
    /**
     * Searches notes by transcript or title containing the query string.
     */
    suspend fun searchNotes(query: String): List<Note>
}
