package com.zarabandajose.offlineaudionotes.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zarabandajose.offlineaudionotes.R
import com.zarabandajose.offlineaudionotes.databinding.ItemNoteBinding
import com.zarabandajose.offlineaudionotes.domain.model.Note
import com.zarabandajose.offlineaudionotes.domain.model.TranscriptionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(note: Note) {
            val context = binding.root.context

            binding.textTitle.text = note.title?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.untitled_note)

            binding.textTranscriptPreview.text = when (note.transcriptionStatus) {
                TranscriptionStatus.IN_PROGRESS -> context.getString(R.string.transcribing_in_background)
                TranscriptionStatus.FAILED -> context.getString(R.string.transcription_failed_message)
                TranscriptionStatus.CANCELLED -> context.getString(R.string.transcription_cancelled)
                else -> note.transcript.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.no_transcript)
            }

            binding.textDate.text = dateFormat.format(Date(note.createdAt))

            binding.textDuration.text = formatDuration(note.durationMillis)
            
            binding.progressTranscribing.visibility = 
                if (note.transcriptionStatus == TranscriptionStatus.IN_PROGRESS) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onNoteClick(note)
            }
        }

        private fun formatDuration(millis: Long?): String {
            if (millis == null || millis <= 0) return "00:00"
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    private class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
