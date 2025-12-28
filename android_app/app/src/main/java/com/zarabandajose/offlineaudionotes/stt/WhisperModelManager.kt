package com.zarabandajose.offlineaudionotes.stt

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages whisper model files for offline STT.
 *
 * Supports two models:
 * - BASE (~148MB): Bundled with the app in assets
 * - SMALL (~465MB): Downloaded on-demand for better quality
 */
object WhisperModelManager {

    private const val MODELS_DIR = "models"
    private const val PREFS_NAME = "whisper_prefs"
    private const val KEY_ACTIVE_MODEL = "active_model"

    /** Available whisper models. */
    enum class ModelType(
            val filename: String,
            val displayName: String,
            val sizeBytes: Long,
            val downloadUrl: String?,
            val isBundled: Boolean
    ) {
        BASE(
                filename = "ggml-base.bin",
                displayName = "Base (~148MB)",
                sizeBytes = 148_000_000L,
                downloadUrl = null,
                isBundled = true
        ),
        SMALL(
                filename = "ggml-small.bin",
                displayName = "Small (~465MB)",
                sizeBytes = 465_000_000L,
                downloadUrl =
                        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
                isBundled = false
        )
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Get the currently active model type. */
    fun getActiveModelType(context: Context): ModelType {
        val modelName = getPrefs(context).getString(KEY_ACTIVE_MODEL, ModelType.BASE.name)
        return try {
            ModelType.valueOf(modelName ?: ModelType.BASE.name)
        } catch (e: IllegalArgumentException) {
            ModelType.BASE
        }
    }

    /** Set the active model type. */
    fun setActiveModelType(context: Context, modelType: ModelType) {
        getPrefs(context).edit().putString(KEY_ACTIVE_MODEL, modelType.name).apply()
    }

    /**
     * Get the absolute path to the active whisper model file. If the model doesn't exist in
     * internal storage (bundled), it will be copied from assets.
     */
    fun getModelPath(context: Context): String? {
        val activeModel = getActiveModelType(context)
        return getModelPath(context, activeModel)
    }

    /** Get path for a specific model type. */
    fun getModelPath(context: Context, modelType: ModelType): String? {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        val modelFile = File(modelsDir, modelType.filename)

        if (modelFile.exists()) {
            return modelFile.absolutePath
        }

        // For bundled models, try to copy from assets
        if (modelType.isBundled) {
            return if (copyModelFromAssets(context, modelType)) {
                modelFile.absolutePath
            } else {
                null
            }
        }

        return null
    }

    /** Check if a specific model is available (downloaded or bundled). */
    fun isModelAvailable(context: Context, modelType: ModelType): Boolean {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        val modelFile = File(modelsDir, modelType.filename)

        if (modelFile.exists()) {
            return true
        }

        // Check if bundled model exists in assets
        if (modelType.isBundled) {
            return try {
                context.assets.open("$MODELS_DIR/${modelType.filename}").use { true }
            } catch (e: IOException) {
                false
            }
        }

        return false
    }

    /**
     * Download a model from the internet with progress callback.
     *
     * @param modelType The model to download
     * @param onProgress Callback with progress (0-100)
     * @return true if download was successful
     */
    suspend fun downloadModel(
            context: Context,
            modelType: ModelType,
            onProgress: (Int) -> Unit
    ): Boolean =
            withContext(Dispatchers.IO) {
                if (modelType.downloadUrl == null) {
                    return@withContext false
                }

                val modelsDir = File(context.filesDir, MODELS_DIR)
                if (!modelsDir.exists() && !modelsDir.mkdirs()) {
                    return@withContext false
                }

                val modelFile = File(modelsDir, modelType.filename)
                val tempFile = File(modelsDir, "${modelType.filename}.tmp")

                try {
                    val url = URL(modelType.downloadUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.connect()

                    val totalSize = connection.contentLength.toLong()
                    var downloadedSize = 0L

                    connection.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedSize += bytesRead
                                val progress =
                                        if (totalSize > 0) {
                                            ((downloadedSize * 100) / totalSize).toInt()
                                        } else {
                                            -1
                                        }
                                onProgress(progress)
                            }
                            output.flush()
                        }
                    }

                    // Rename temp file to final file
                    if (tempFile.renameTo(modelFile)) {
                        return@withContext true
                    } else {
                        tempFile.delete()
                        return@withContext false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    tempFile.delete()
                    return@withContext false
                }
            }

    /** Delete a downloaded model to free space. */
    fun deleteModel(context: Context, modelType: ModelType): Boolean {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        val modelFile = File(modelsDir, modelType.filename)

        if (modelFile.exists()) {
            val deleted = modelFile.delete()
            // If deleting active model, switch back to base
            if (deleted && getActiveModelType(context) == modelType) {
                setActiveModelType(context, ModelType.BASE)
            }
            return deleted
        }
        return true
    }

    /** Copy a bundled model from assets to internal storage. */
    private fun copyModelFromAssets(context: Context, modelType: ModelType): Boolean {
        if (!modelType.isBundled) return false

        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists() && !modelsDir.mkdirs()) {
            return false
        }

        val modelFile = File(modelsDir, modelType.filename)

        return try {
            context.assets.open("$MODELS_DIR/${modelType.filename}").use { inputStream ->
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

    /** Get the size of a model file in bytes, or -1 if not available. */
    fun getModelSize(context: Context, modelType: ModelType): Long {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        val modelFile = File(modelsDir, modelType.filename)
        return if (modelFile.exists()) {
            modelFile.length()
        } else {
            -1
        }
    }

    /** Get total storage used by all models. */
    fun getTotalStorageUsed(context: Context): Long {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists()) return 0

        return modelsDir.listFiles()?.sumOf { it.length() } ?: 0
    }
}
