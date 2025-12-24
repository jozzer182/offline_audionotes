package com.zarabandajose.offlineaudionotes.stt

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages the whisper model file for offline STT.
 *
 * The model file is expected to be placed in the app's assets folder at:
 * assets/models/ggml-base.bin
 *
 * On first use, the model is copied from assets to the app's internal storage for faster access by
 * the native code.
 *
 * Developer Instructions:
 * 1. Download a whisper model (e.g., ggml-base.bin) from:
 * ```
 *    https://huggingface.co/ggerganov/whisper.cpp/tree/main
 * ```
 * 2. Place it in: android_app/app/src/main/assets/models/ggml-base.bin
 * 3. The app will automatically copy it to internal storage on first launch
 */
object WhisperModelManager {

    private const val MODELS_DIR = "models"
    private const val MODEL_FILENAME = "ggml-base.bin"
    private const val ASSET_MODEL_PATH = "$MODELS_DIR/$MODEL_FILENAME"

    /**
     * Get the absolute path to the whisper model file. If the model doesn't exist in internal
     * storage, it will be copied from assets.
     *
     * @param context Application context
     * @return Absolute path to the model file, or null if the model is not available
     */
    fun getModelPath(context: Context): String? {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        val modelFile = File(modelsDir, MODEL_FILENAME)

        if (modelFile.exists()) {
            return modelFile.absolutePath
        }

        // Try to copy from assets
        return if (copyModelFromAssets(context)) {
            modelFile.absolutePath
        } else {
            null
        }
    }

    /** Check if the whisper model is available (either in internal storage or assets). */
    fun isModelAvailable(context: Context): Boolean {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        val modelFile = File(modelsDir, MODEL_FILENAME)

        if (modelFile.exists()) {
            return true
        }

        // Check if model exists in assets
        return try {
            context.assets.open(ASSET_MODEL_PATH).use { true }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Copy the model file from assets to internal storage. This is done once on first use for
     * faster native file access.
     *
     * @return true if copy was successful, false otherwise
     */
    fun copyModelFromAssets(context: Context): Boolean {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists() && !modelsDir.mkdirs()) {
            return false
        }

        val modelFile = File(modelsDir, MODEL_FILENAME)

        return try {
            context.assets.open(ASSET_MODEL_PATH).use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete the cached model file from internal storage. Useful for freeing up space or forcing a
     * re-copy from assets.
     */
    fun deleteModelCache(context: Context): Boolean {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        val modelFile = File(modelsDir, MODEL_FILENAME)
        return if (modelFile.exists()) {
            modelFile.delete()
        } else {
            true
        }
    }

    /** Get the size of the model file in bytes, or -1 if not available. */
    fun getModelSize(context: Context): Long {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        val modelFile = File(modelsDir, MODEL_FILENAME)
        return if (modelFile.exists()) {
            modelFile.length()
        } else {
            -1
        }
    }
}
