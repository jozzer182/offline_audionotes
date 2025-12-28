package com.zarabandajose.offlineaudionotes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zarabandajose.offlineaudionotes.data.repository.NotesRepositoryProvider
import com.zarabandajose.offlineaudionotes.databinding.ActivityMainBinding
import com.zarabandajose.offlineaudionotes.domain.model.Note
import com.zarabandajose.offlineaudionotes.ui.NoteDetailActivity
import com.zarabandajose.offlineaudionotes.ui.adapter.NotesAdapter
import com.zarabandajose.offlineaudionotes.worker.TranscribeNoteWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var notesAdapter: NotesAdapter
    private var searchJob: Job? = null
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotesRepositoryProvider.init(applicationContext)
        TranscribeNoteWorker.createNotificationChannel(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        setupFab()
        setupSettings()
        observeNotes()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter { note -> openNoteDetail(note.id) }
        binding.recyclerNotes.adapter = notesAdapter
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {}
                    override fun afterTextChanged(s: Editable?) {
                        val query = s?.toString() ?: ""
                        searchWithDebounce(query)
                    }
                }
        )
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener { openNoteDetail(NoteDetailActivity.NEW_NOTE_ID) }
    }

    private fun setupSettings() {
        binding.btnSettings.setOnClickListener {
            startActivity(
                    android.content.Intent(
                            this,
                            com.zarabandajose.offlineaudionotes.ui.SettingsActivity::class.java
                    )
            )
        }
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotesRepositoryProvider.repository.searchNotesFlow(currentQuery).collectLatest {
                        notes ->
                    updateNotesList(notes)
                }
            }
        }
    }

    private fun searchWithDebounce(query: String) {
        searchJob?.cancel()
        searchJob =
                lifecycleScope.launch {
                    delay(300)
                    currentQuery = query
                    NotesRepositoryProvider.repository.searchNotesFlow(query).collectLatest { notes
                        ->
                        updateNotesList(notes)
                    }
                }
    }

    private fun updateNotesList(notes: List<Note>) {
        notesAdapter.submitList(notes)

        val isEmpty = notes.isEmpty()
        val hasQuery = currentQuery.isNotBlank()

        binding.emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerNotes.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (isEmpty) {
            binding.textEmptyState.text =
                    if (hasQuery) {
                        getString(R.string.no_search_results)
                    } else {
                        getString(R.string.empty_notes_message)
                    }
        }
    }

    private fun openNoteDetail(noteId: Long) {
        startActivity(NoteDetailActivity.newIntent(this, noteId))
    }
}
