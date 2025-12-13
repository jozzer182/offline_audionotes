package com.zarabandajose.offlineaudionotes.audio

import android.media.MediaPlayer
import java.io.IOException

/**
 * Manages audio playback using MediaPlayer.
 * 
 * MediaPlayer is chosen for its simplicity and suitability for this lightweight app.
 * It handles AAC/M4A files natively on Android.
 */
class AudioPlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var currentFilePath: String? = null

    var onPlaybackCompleted: (() -> Unit)? = null
    var onPlaybackError: ((String) -> Unit)? = null
    var onProgressUpdate: ((currentMs: Int, totalMs: Int) -> Unit)? = null

    /**
     * Prepares the player with the given audio file.
     * @return true if preparation was successful, false otherwise.
     */
    fun prepare(filePath: String): Boolean {
        if (currentFilePath == filePath && isPrepared) {
            return true
        }

        release()
        currentFilePath = filePath

        return try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    onPlaybackCompleted?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    onPlaybackError?.invoke("Playback error: $what, $extra")
                    true
                }
                prepare()
            }
            isPrepared = true
            true
        } catch (e: IOException) {
            e.printStackTrace()
            onPlaybackError?.invoke("Cannot load audio file")
            false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            onPlaybackError?.invoke("Player in invalid state")
            false
        }
    }

    /**
     * Starts or resumes playback.
     */
    fun play(): Boolean {
        if (!isPrepared || mediaPlayer == null) {
            return false
        }
        try {
            mediaPlayer?.start()
            return true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Pauses playback.
     */
    fun pause(): Boolean {
        if (!isPrepared || mediaPlayer == null) {
            return false
        }
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
            return true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Stops playback and resets to the beginning.
     */
    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                prepare()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Seeks to a specific position in milliseconds.
     */
    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    /**
     * Returns the current playback position in milliseconds.
     */
    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: IllegalStateException) {
            0
        }
    }

    /**
     * Returns the total duration in milliseconds.
     */
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: IllegalStateException) {
            0
        }
    }

    /**
     * Returns true if currently playing.
     */
    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Releases all resources. Call this when done with the player.
     */
    fun release() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        isPrepared = false
        currentFilePath = null
    }
}
