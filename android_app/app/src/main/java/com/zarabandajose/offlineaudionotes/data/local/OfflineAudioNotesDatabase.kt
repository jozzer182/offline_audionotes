package com.zarabandajose.offlineaudionotes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for the Offline Audio Notes app.
 * 
 * Contains a single table for notes with their audio paths and transcripts.
 * Uses destructive migration during development - in production, proper
 * migrations should be implemented.
 */
@Database(
    entities = [NoteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class OfflineAudioNotesDatabase : RoomDatabase() {

    abstract fun notesDao(): NotesDao

    companion object {
        private const val DATABASE_NAME = "offline_audio_notes.db"

        @Volatile
        private var INSTANCE: OfflineAudioNotesDatabase? = null

        /**
         * Get the singleton database instance.
         * Uses double-checked locking for thread safety.
         */
        fun getInstance(context: Context): OfflineAudioNotesDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): OfflineAudioNotesDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                OfflineAudioNotesDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
