package com.zarabandajose.offlineaudionotes.stt

/**
 * JNI Bridge to whisper.cpp for offline speech-to-text transcription.
 * 
 * This singleton provides access to the native whisper.cpp library for
 * transcribing audio files to text completely offline.
 * 
 * Usage:
 * 1. Ensure the whisper model is available via WhisperModelManager
 * 2. Call transcribeFile() with the model path, audio path, and language
 * 3. The function returns the transcript or an error message starting with "ERROR:"
 */
object WhisperBridge {

    init {
        System.loadLibrary("offlineaudionotes")
    }

    /**
     * Transcribe an audio file to text using whisper.cpp.
     * 
     * @param modelPath Absolute path to the whisper model file (e.g., ggml-base.bin)
     * @param audioPath Absolute path to the audio file (WAV format, 16kHz, mono, 16-bit PCM)
     * @param language Language code for transcription (e.g., "en", "es", "auto")
     * @return The transcribed text, or an error message starting with "ERROR:"
     */
    external fun transcribeFile(modelPath: String, audioPath: String, language: String): String

    /**
     * Release the cached whisper model from memory.
     * Call this when the app is being destroyed or when memory is needed.
     */
    external fun releaseModel()

    /**
     * Check if the transcription result is an error.
     */
    fun isError(result: String): Boolean {
        return result.startsWith("ERROR:")
    }

    /**
     * Extract error message from result.
     */
    fun getErrorMessage(result: String): String {
        return if (isError(result)) {
            result.removePrefix("ERROR:").trim()
        } else {
            ""
        }
    }
}
