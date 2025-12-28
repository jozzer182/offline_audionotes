package com.zarabandajose.offlineaudionotes.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zarabandajose.offlineaudionotes.R
import com.zarabandajose.offlineaudionotes.databinding.ActivitySettingsBinding
import com.zarabandajose.offlineaudionotes.stt.WhisperModelManager
import com.zarabandajose.offlineaudionotes.stt.WhisperModelManager.ModelType
import java.text.DecimalFormat
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var isDownloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupModelSelection()
        updateUI()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupModelSelection() {
        // Base model selection
        binding.cardModelBase.setOnClickListener { selectModel(ModelType.BASE) }
        binding.radioBase.setOnClickListener { selectModel(ModelType.BASE) }

        // Small model selection
        binding.cardModelSmall.setOnClickListener {
            if (WhisperModelManager.isModelAvailable(this, ModelType.SMALL)) {
                selectModel(ModelType.SMALL)
            }
        }
        binding.radioSmall.setOnClickListener {
            if (WhisperModelManager.isModelAvailable(this, ModelType.SMALL)) {
                selectModel(ModelType.SMALL)
            }
        }

        // Download button
        binding.btnDownloadSmall.setOnClickListener { downloadSmallModel() }

        // Delete button
        binding.btnDeleteSmall.setOnClickListener { deleteSmallModel() }
    }

    private fun selectModel(modelType: ModelType) {
        if (isDownloading) return

        WhisperModelManager.setActiveModelType(this, modelType)
        updateUI()

        Toast.makeText(
                        this,
                        getString(R.string.model_selected, modelType.displayName),
                        Toast.LENGTH_SHORT
                )
                .show()
    }

    private fun downloadSmallModel() {
        if (isDownloading) return

        isDownloading = true
        binding.layoutDownloadProgress.visibility = View.VISIBLE
        binding.btnDownloadSmall.visibility = View.GONE
        binding.progressDownload.progress = 0
        binding.textDownloadProgress.text = getString(R.string.downloading_model, 0)

        lifecycleScope.launch {
            val success =
                    WhisperModelManager.downloadModel(this@SettingsActivity, ModelType.SMALL) {
                            progress ->
                        runOnUiThread {
                            binding.progressDownload.progress = progress
                            binding.textDownloadProgress.text =
                                    getString(R.string.downloading_model, progress)
                        }
                    }

            runOnUiThread {
                isDownloading = false
                binding.layoutDownloadProgress.visibility = View.GONE

                if (success) {
                    Toast.makeText(
                                    this@SettingsActivity,
                                    R.string.model_download_complete,
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                    // Auto-select the downloaded model
                    selectModel(ModelType.SMALL)
                } else {
                    Toast.makeText(
                                    this@SettingsActivity,
                                    R.string.model_download_failed,
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
                updateUI()
            }
        }
    }

    private fun deleteSmallModel() {
        if (isDownloading) return

        val deleted = WhisperModelManager.deleteModel(this, ModelType.SMALL)
        if (deleted) {
            Toast.makeText(this, R.string.model_deleted, Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }

    private fun updateUI() {
        val activeModel = WhisperModelManager.getActiveModelType(this)
        val smallModelAvailable = WhisperModelManager.isModelAvailable(this, ModelType.SMALL)
        val storageUsed = WhisperModelManager.getTotalStorageUsed(this)

        // Update current model text
        binding.textCurrentModel.text = getString(R.string.current_model, activeModel.displayName)
        binding.textStorageUsed.text =
                getString(R.string.storage_usage, formatFileSize(storageUsed))

        // Update radio buttons
        binding.radioBase.isChecked = activeModel == ModelType.BASE
        binding.radioSmall.isChecked = activeModel == ModelType.SMALL
        binding.radioSmall.isEnabled = smallModelAvailable

        // Update small model buttons
        if (smallModelAvailable) {
            binding.btnDownloadSmall.visibility = View.GONE
            binding.btnDeleteSmall.visibility = View.VISIBLE
        } else {
            binding.btnDownloadSmall.visibility = View.VISIBLE
            binding.btnDeleteSmall.visibility = View.GONE
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.#")
        return df.format(bytes / Math.pow(1024.0, digitGroups.toDouble())) +
                " " +
                units[digitGroups]
    }
}
