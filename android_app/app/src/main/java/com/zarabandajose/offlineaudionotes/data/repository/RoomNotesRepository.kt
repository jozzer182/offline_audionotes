package com.zarabandajose.offlineaudionotes.data.repository

import com.zarabandajose.offlineaudionotes.data.local.NoteEntity
import com.zarabandajose.offlineaudionotes.data.local.NotesDao
import com.zarabandajose.offlineaudionotes.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-based implementation of NotesRepository.
 * Provides persistent storage for notes using the local SQLite database.
 */
class RoomNotesRepository(
    private val notesDao: NotesDao
) : NotesRepository {

    /**
     * Get all notes as a reactive Flow.
     */
    fun getAllNotesFlow(): Flow<List<Note>> {
        return notesDao.getAllNotes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Search notes by query (title or transcript).
     */
    fun searchNotesFlow(query: String): Flow<List<Note>> {
        return if (query.isBlank()) {
            getAllNotesFlow()
        } else {
            notesDao.searchNotes(query).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }
    }

    override suspend fun getAllNotes(): List<Note> {
        throw UnsupportedOperationException("Use getAllNotesFlow() instead")
    }

    override suspend fun searchNotes(query: String): List<Note> {
        throw UnsupportedOperationException("Use searchNotesFlow() instead")
    }

    override suspend fun getNoteById(id: Long): Note? {
        return notesDao.getNoteById(id)?.toDomainModel()
    }

    /**
     * Get a note by ID as a reactive Flow.
     */
    fun getNoteByIdFlow(id: Long): Flow<Note?> {
        return notesDao.getNoteByIdFlow(id).map { it?.toDomainModel() }
    }

    override suspend fun saveNote(note: Note): Note {
        val entity = NoteEntity.fromDomainModel(note)
        val id = notesDao.insertNote(entity)
        return note.copy(id = id)
    }

    override suspend fun updateNote(note: Note) {
        val entity = NoteEntity.fromDomainModel(note)
        notesDao.updateNote(entity)
    }

    override suspend fun deleteNote(id: Long) {
        notesDao.deleteNoteById(id)
    }

    /**
     * Get the count of all notes.
     */
    suspend fun getNotesCount(): Int {
        return notesDao.getNotesCount()
    }
}
