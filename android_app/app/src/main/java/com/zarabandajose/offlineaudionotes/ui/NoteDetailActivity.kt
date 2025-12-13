package com.zarabandajose.offlineaudionotes.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zarabandajose.offlineaudionotes.R
import com.zarabandajose.offlineaudionotes.audio.AudioPlayerManager
import com.zarabandajose.offlineaudionotes.audio.AudioRecorderManager
import com.zarabandajose.offlineaudionotes.data.repository.NotesRepositoryProvider
import com.zarabandajose.offlineaudionotes.databinding.ActivityNoteDetailBinding
import com.zarabandajose.offlineaudionotes.domain.model.Note
import com.zarabandajose.offlineaudionotes.domain.model.TranscriptionStatus
import com.zarabandajose.offlineaudionotes.ui.components.WavyAudioPlayer
import com.zarabandajose.offlineaudionotes.worker.TranscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private var noteId: Long = NEW_NOTE_ID
    private var currentNote: Note? = null

    private lateinit var audioRecorder: AudioRecorderManager
    private lateinit var audioPlayer: AudioPlayerManager

    private var isRecording = false
    private var currentAudioPath: String? = null
    private var currentDurationMillis: Long = 0
    private var currentLanguage: String = "en"

    // Compose state for WavyAudioPlayer
    private val isPlayingState = mutableStateOf(false)
    private val currentPositionState = mutableLongStateOf(0L)
    private val totalDurationState = mutableLongStateOf(0L)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enqueueTranscriptionForCurrentNote()
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val recordingTimerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                updateRecordingTimer()
                handler.postDelayed(this, 1000)
            }
        }
    }
    private val playbackProgressRunnable = object : Runnable {
        override fun run() {
            if (isPlayingState.value) {
                updatePlaybackProgress()
                handler.postDelayed(this, 100)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteId = intent.getLongExtra(EXTRA_NOTE_ID, NEW_NOTE_ID)

        audioRecorder = AudioRecorderManager(this)
        audioPlayer = AudioPlayerManager()

        setupUI()
        setupComposeAudioPlayer()
        setupAudioPlayerCallbacks()
        loadNote()
        observeNote()
    }

    override fun onPause() {
        super.onPause()
        stopRecordingIfActive()
        stopPlaybackIfActive()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(recordingTimerRunnable)
        handler.removeCallbacks(playbackProgressRunnable)
        audioRecorder.release()
        audioPlayer.release()
    }

    private fun setupComposeAudioPlayer() {
        binding.composeAudioPlayer.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WavyAudioPlayer(
                    isPlaying = isPlayingState.value,
                    currentPositionMs = currentPositionState.longValue,
                    totalDurationMs = totalDurationState.longValue,
                    onPlayPauseClick = { togglePlayback() },
                    onSeek = { /* Seek not supported in wavy indicator */ },
                    onShareClick = { shareAudio() }
                )
            }
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRecord.setOnClickListener {
            toggleRecording()
        }

        binding.btnTranscribe.setOnClickListener {
            requestTranscription()
        }

        binding.btnShareText.setOnClickListener {
            shareText()
        }
    }

    private fun observeNote() {
        if (noteId == NEW_NOTE_ID) return
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotesRepositoryProvider.repository.getNoteByIdFlow(noteId)
                    .collectLatest { note ->
                        if (note != null) {
                            currentNote = note
                            updateTranscriptionStatusUI(note)
                        }
                    }
            }
        }
    }

    private fun setupAudioPlayerCallbacks() {
        audioPlayer.onPlaybackCompleted = {
            runOnUiThread {
                isPlayingState.value = false
                currentPositionState.longValue = 0L
                handler.removeCallbacks(playbackProgressRunnable)
            }
        }

        audioPlayer.onPlaybackError = { error ->
            runOnUiThread {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                isPlayingState.value = false
            }
        }
    }

    private fun loadNote() {
        if (noteId == NEW_NOTE_ID) {
            showNewNoteMode()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val note = NotesRepositoryProvider.repository.getNoteById(noteId)
            withContext(Dispatchers.Main) {
                if (note != null) {
                    currentNote = note
                    currentAudioPath = note.audioPath.takeIf { it.isNotBlank() }
                    currentDurationMillis = note.durationMillis ?: 0
                    displayNote(note)
                } else {
                    showNewNoteMode()
                }
            }
        }
    }

    private fun displayNote(note: Note) {
        binding.textTitle.text = note.title?.takeIf { it.isNotBlank() }
            ?: getString(R.string.untitled_note)

        val hasAudio = note.audioPath.isNotBlank() && File(note.audioPath).exists()

        if (hasAudio) {
            binding.composeAudioPlayer.visibility = View.VISIBLE
            binding.cardRecordingControls.visibility = View.GONE
            totalDurationState.longValue = note.durationMillis ?: 0
            currentPositionState.longValue = 0L
            preparePlayback(note.audioPath)
        } else {
            binding.composeAudioPlayer.visibility = View.GONE
            binding.cardRecordingControls.visibility = View.VISIBLE
            binding.textRecordingTime.text = "00:00"
        }

        binding.labelTranscript.visibility = View.VISIBLE
        binding.scrollTranscript.visibility = View.VISIBLE
        binding.textNoAudio.visibility = View.GONE
        currentLanguage = note.language
        
        updateTranscriptionStatusUI(note)
    }

    private fun updateTranscriptionStatusUI(note: Note) {
        val hasAudio = note.audioPath.isNotBlank() && File(note.audioPath).exists()
        
        when (note.transcriptionStatus) {
            TranscriptionStatus.PENDING -> {
                binding.textTranscript.text = getString(R.string.transcript_pending)
                binding.btnTranscribe.visibility = if (hasAudio) View.VISIBLE else View.GONE
                binding.progressTranscribe.visibility = View.GONE
                binding.btnShareText.visibility = View.GONE
            }
            TranscriptionStatus.IN_PROGRESS -> {
                binding.textTranscript.text = getString(R.string.transcribing_in_background)
                binding.btnTranscribe.visibility = View.GONE
                binding.progressTranscribe.visibility = View.VISIBLE
                binding.btnShareText.visibility = View.GONE
            }
            TranscriptionStatus.COMPLETED -> {
                binding.textTranscript.text = note.transcript.takeIf { it.isNotBlank() }
                    ?: getString(R.string.no_transcript)
                binding.btnTranscribe.visibility = if (hasAudio) View.VISIBLE else View.GONE
                binding.progressTranscribe.visibility = View.GONE
                val hasValidTranscript = note.transcript.isNotBlank()
                binding.btnShareText.visibility = if (hasValidTranscript) View.VISIBLE else View.GONE
            }
            TranscriptionStatus.FAILED -> {
                val errorText = note.lastTranscriptionError?.let {
                    getString(R.string.transcription_failed_message) + ": " + it
                } ?: getString(R.string.transcription_failed_message)
                binding.textTranscript.text = errorText
                binding.btnTranscribe.visibility = if (hasAudio) View.VISIBLE else View.GONE
                binding.progressTranscribe.visibility = View.GONE
                binding.btnShareText.visibility = View.GONE
            }
            TranscriptionStatus.CANCELLED -> {
                binding.textTranscript.text = getString(R.string.transcription_cancelled)
                binding.btnTranscribe.visibility = if (hasAudio) View.VISIBLE else View.GONE
                binding.progressTranscribe.visibility = View.GONE
                binding.btnShareText.visibility = View.GONE
            }
        }
        
        if (note.title != null) {
            binding.textTitle.text = note.title
        }
    }

    private fun showNewNoteMode() {
        binding.textTitle.text = getString(R.string.new_note)
        binding.composeAudioPlayer.visibility = View.GONE
        binding.cardRecordingControls.visibility = View.VISIBLE
        binding.labelTranscript.visibility = View.GONE
        binding.scrollTranscript.visibility = View.GONE
        binding.textNoAudio.visibility = View.GONE
        binding.btnTranscribe.visibility = View.GONE
        binding.textRecordingTime.text = "00:00"
        binding.textRecordingStatus.text = getString(R.string.tap_to_record)
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            checkPermissionAndRecord()
        }
    }

    private fun checkPermissionAndRecord() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        val filePath = audioRecorder.startRecording()
        if (filePath != null) {
            isRecording = true
            currentAudioPath = filePath
            updateRecordingUI(true)
            handler.post(recordingTimerRunnable)
        } else {
            Toast.makeText(this, R.string.recording_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        val result = audioRecorder.stopRecording()
        isRecording = false
        handler.removeCallbacks(recordingTimerRunnable)

        if (result != null) {
            currentAudioPath = result.filePath
            currentDurationMillis = result.durationMillis
            updateRecordingUI(false)
            saveNoteWithAudio(result.filePath, result.durationMillis)
        }
    }

    private fun stopRecordingIfActive() {
        if (isRecording) {
            stopRecording()
        }
    }

    private fun updateRecordingUI(recording: Boolean) {
        if (recording) {
            binding.btnRecord.setImageResource(R.drawable.ic_stop)
            binding.textRecordingStatus.text = getString(R.string.recording)
            binding.recordingIndicator.visibility = View.VISIBLE
        } else {
            binding.btnRecord.setImageResource(R.drawable.ic_mic)
            binding.textRecordingStatus.text = getString(R.string.tap_to_record)
            binding.recordingIndicator.visibility = View.GONE
        }
    }

    private fun updateRecordingTimer() {
        val durationMs = audioRecorder.getRecordingDurationMillis()
        binding.textRecordingTime.text = formatDuration(durationMs)
    }

    private fun saveNoteWithAudio(audioPath: String, durationMillis: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            if (noteId == NEW_NOTE_ID) {
                val newNote = Note(
                    id = 0,
                    createdAt = System.currentTimeMillis(),
                    title = null,
                    transcript = getString(R.string.transcript_pending),
                    audioPath = audioPath,
                    durationMillis = durationMillis,
                    language = "en"
                )
                val savedNote = NotesRepositoryProvider.repository.saveNote(newNote)
                withContext(Dispatchers.Main) {
                    noteId = savedNote.id
                    currentNote = savedNote
                    observeNote()
                    onAudioRecorded(savedNote.id, audioPath)
                    showPlaybackMode()
                }
            } else {
                currentNote?.let { note ->
                    val updatedNote = note.copy(
                        audioPath = audioPath,
                        durationMillis = durationMillis
                    )
                    NotesRepositoryProvider.repository.updateNote(updatedNote)
                    withContext(Dispatchers.Main) {
                        currentNote = updatedNote
                        onAudioRecorded(updatedNote.id, audioPath)
                        showPlaybackMode()
                    }
                }
            }
        }
    }

    private fun onAudioRecorded(noteId: Long, audioPath: String) {
        checkNotificationPermissionAndTranscribe()
    }

    private fun requestTranscription() {
        val status = currentNote?.transcriptionStatus
        if (status == TranscriptionStatus.IN_PROGRESS) {
            Toast.makeText(this, R.string.transcription_already_running, Toast.LENGTH_SHORT).show()
            return
        }
        checkNotificationPermissionAndTranscribe()
    }

    private fun checkNotificationPermissionAndTranscribe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    enqueueTranscriptionForCurrentNote()
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            enqueueTranscriptionForCurrentNote()
        }
    }

    private fun enqueueTranscriptionForCurrentNote() {
        val audioPath = currentAudioPath
        if (audioPath == null || !File(audioPath).exists()) {
            Toast.makeText(this, R.string.audio_file_missing, Toast.LENGTH_SHORT).show()
            return
        }

        if (noteId == NEW_NOTE_ID) {
            Toast.makeText(this, R.string.transcription_error, Toast.LENGTH_SHORT).show()
            return
        }

        TranscriptionManager.enqueueTranscription(
            context = this,
            noteId = noteId,
            audioPath = audioPath,
            language = currentLanguage
        )

        Toast.makeText(this, R.string.transcribing_in_background, Toast.LENGTH_SHORT).show()
    }

    private fun cancelTranscription() {
        if (noteId != NEW_NOTE_ID) {
            TranscriptionManager.cancelTranscription(this, noteId)
        }
    }

    private fun showPlaybackMode() {
        binding.cardRecordingControls.visibility = View.GONE
        binding.composeAudioPlayer.visibility = View.VISIBLE
        binding.labelTranscript.visibility = View.VISIBLE
        binding.scrollTranscript.visibility = View.VISIBLE
        binding.textTranscript.text = getString(R.string.transcript_pending)
        
        totalDurationState.longValue = currentDurationMillis
        currentPositionState.longValue = 0L

        currentAudioPath?.let { preparePlayback(it) }
    }

    private fun preparePlayback(audioPath: String) {
        if (audioPlayer.prepare(audioPath)) {
            totalDurationState.longValue = audioPlayer.getDuration().toLong()
        }
    }

    private fun togglePlayback() {
        if (isPlayingState.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        currentAudioPath?.let { path ->
            if (!audioPlayer.isPlaying()) {
                if (audioPlayer.prepare(path) && audioPlayer.play()) {
                    isPlayingState.value = true
                    handler.post(playbackProgressRunnable)
                }
            }
        }
    }

    private fun pausePlayback() {
        audioPlayer.pause()
        isPlayingState.value = false
        handler.removeCallbacks(playbackProgressRunnable)
    }

    private fun stopPlaybackIfActive() {
        if (isPlayingState.value) {
            audioPlayer.pause()
            isPlayingState.value = false
            handler.removeCallbacks(playbackProgressRunnable)
        }
    }

    private fun updatePlaybackProgress() {
        val current = audioPlayer.getCurrentPosition()
        currentPositionState.longValue = current.toLong()
    }

    private fun formatDuration(millis: Long?): String {
        if (millis == null || millis <= 0) return "00:00"
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun shareText() {
        val transcript = currentNote?.transcript
        if (transcript.isNullOrBlank() || transcript == getString(R.string.transcript_pending) || transcript == getString(R.string.no_transcript)) {
            Toast.makeText(this, R.string.no_text_to_share, Toast.LENGTH_SHORT).show()
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, transcript)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_note)))
    }

    private fun shareAudio() {
        val audioPath = currentAudioPath
        if (audioPath.isNullOrBlank() || !File(audioPath).exists()) {
            Toast.makeText(this, R.string.audio_file_missing, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val audioFile = File(audioPath)
            val audioUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                audioFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, audioUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_note)))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.audio_file_missing, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EXTRA_NOTE_ID = "extra_note_id"
        const val NEW_NOTE_ID = -1L
        private const val MAX_TITLE_LENGTH = 50

        fun newIntent(context: Context, noteId: Long = NEW_NOTE_ID): Intent {
            return Intent(context, NoteDetailActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
            }
        }
    }
}
