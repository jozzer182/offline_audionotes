package com.zarabandajose.offlineaudionotes.data.repository

import android.content.Context
import com.zarabandajose.offlineaudionotes.data.local.OfflineAudioNotesDatabase

/**
 * Singleton provider for NotesRepository.
 * Uses Room-based repository for persistent storage.
 * Must be initialized with application context before use.
 */
object NotesRepositoryProvider {
    
    private var _repository: RoomNotesRepository? = null
    
    /**
     * Get the Room-based repository instance.
     * Throws if not initialized.
     */
    val repository: RoomNotesRepository
        get() = _repository ?: throw IllegalStateException(
            "NotesRepositoryProvider not initialized. Call init(context) first."
        )
    
    /**
     * Initialize the repository with application context.
     * Should be called once from Application.onCreate() or MainActivity.
     */
    fun init(context: Context) {
        if (_repository == null) {
            val database = OfflineAudioNotesDatabase.getInstance(context)
            _repository = RoomNotesRepository(database.notesDao())
        }
    }
    
    /**
     * Check if the repository has been initialized.
     */
    fun isInitialized(): Boolean = _repository != null
}
