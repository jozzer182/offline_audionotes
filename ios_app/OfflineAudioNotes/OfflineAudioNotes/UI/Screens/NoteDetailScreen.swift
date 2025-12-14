//
//  NoteDetailScreen.swift
//  OfflineAudioNotes
//
//  Created by JOSE ZARABANDA on 12/13/25.
//

import SwiftData
import AVFoundation
import SwiftUI

struct NoteDetailScreen: View {
    @Bindable var note: Note
    @State private var audioRecorder = AudioRecorder()
    @State private var audioPlayer = AudioPlayer()
    // @State private var showShareSheet = false // Replaced by shareItems
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Title Editable
                TextField("Note Title", text: Binding(
                    get: { note.title ?? "" },
                    set: { note.title = $0 }
                ))
                .font(.title2.bold())
                .padding()
                .glassCard()
                .padding(.horizontal)
                
                if let transcript = note.transcript, !transcript.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("TRANSCRIPT")
                                .font(.headline) // Bigger and Bolder (User request)
                                .fontWeight(.bold)
                                .foregroundStyle(AppColors.primary)
                            
                            Spacer()
                            
                            Button(action: {
                                showShareSheet(items: [transcript])
                            }) {
                                Image(systemName: "square.and.arrow.up")
                                    .font(.title3.bold()) // Stronger and bigger (Match Audio share)
                                    .foregroundStyle(AppColors.primary)
                                    .padding(12) // Unified padding
                                    .background(AppColors.background.opacity(0.5))
                                    .clipShape(Circle())
                            }
                        }

                        
                        Text(transcript)
                            .font(.body)
                            .lineSpacing(6)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(20) // Unified padding for the whole card content
                    .glassCard()
                    .padding(.horizontal) // External padding
                }
                
                // Status & Cancel Action
                Group {
                    if note.status == .processing || note.status == .pending {
                        HStack {
                            // Shimmering Placeholder Icon
                            RoundedRectangle(cornerRadius: 8)
                                .fill(AppColors.primary.opacity(0.1))
                                .frame(width: 24, height: 24)
                                .shimmer()
                                .padding(.trailing, 8)
                            
                            // Static Text (User feedback: removed shimmer for readability)
                            Text(note.status == .processing ? "Transcribing..." : "Pending...")
                                .foregroundStyle(.secondary)
                            
                            Spacer()
                            
                            Button("Cancel") {
                                cancelTranscription()
                            }
                            .buttonStyle(.bordered)
                            .tint(AppColors.warning)
                        }
                        .padding(.horizontal)
                        .transition(.opacity)
                    } else if note.status == .cancelled {
                        Text("Transcription Cancelled")
                            .font(.caption)
                            .foregroundStyle(AppColors.warning)
                            .padding(.horizontal)
                    }
                }
                .padding(.horizontal)
                .animation(.easeInOut, value: note.status)
                
                // Audio Controls Section
                VStack(spacing: 16) {
                    if let audioPath = note.audioPath {
                        // Playback UI
                        VStack(spacing: 12) {
                            HStack {
                                Button(action: {
                                    if audioPlayer.isPlaying {
                                        audioPlayer.pausePlayback()
                                    } else {
                                        audioPlayer.startPlayback(from: audioPath)
                                    }
                                }) {
                                    ZStack {
                                        Circle()
                                            .fill(AppColors.primary.opacity(0.2))
                                            .frame(width: 60, height: 60)
                                            .glassEffect() // Apply glass effect to background
                                        
                                        Image(systemName: audioPlayer.isPlaying ? "pause.fill" : "play.fill")
                                            .font(.title)
                                            .foregroundStyle(.primary) // High contrast (Black/White) vs light background
                                    }
                                    .scaleEffect(audioPlayer.isPlaying ? 1.1 : 1.0)
                                    .animation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true), value: audioPlayer.isPlaying)
                                }
                                .buttonStyle(PlainButtonStyle()) // explicit plain style
                                
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Audio Note")
                                        .font(.headline)
                                    Text(formatTime(audioPlayer.currentTime) + " / " + formatTime(audioPlayer.duration > 0 ? audioPlayer.duration : Double(note.durationMillis ?? 0) / 1000.0))
                                        .font(.caption)
                                        .monospacedDigit()
                                        .foregroundStyle(.secondary)
                                }
                                
                                Spacer()
                                
                                Button(action: {
                                    if let audioUrl = getAudioUrl(for: audioPath) {
                                        showShareSheet(items: [audioUrl])
                                    }
                                }) {
                                    Image(systemName: "square.and.arrow.up")
                                        .font(.title3.bold()) // Stronger
                                        .foregroundStyle(AppColors.primary)
                                        .padding(12)
                                        .background(AppColors.background.opacity(0.5))
                                        .clipShape(Circle())
                                }
                            }
                            
                            if audioPlayer.duration > 0 {
                                Slider(value: Binding(
                                    get: { audioPlayer.currentTime },
                                    set: { audioPlayer.seek(to: $0) }
                                ), in: 0...audioPlayer.duration)
                                .tint(AppColors.primary)
                            }
                        }
                        .padding()
                        .glassCard()
                    } else {
                        // Recording UI
                        VStack(spacing: 16) {
                            if audioRecorder.isRecording {
                                Text(formatTime(audioRecorder.recordingTime))
                                    .font(.system(size: 48, weight: .thin).monospacedDigit())
                                    .contentTransition(.numericText())
                                    .foregroundStyle(AppColors.warning)
                                
                                Button(action: stopRecording) {
                                    Image(systemName: "stop.fill")
                                        .font(.system(size: 32))
                                        .foregroundStyle(.white)
                                        .frame(width: 72, height: 72)
                                        .background(AppColors.warning)
                                        .clipShape(Circle())
                                        .shadow(color: AppColors.warning.opacity(0.4), radius: 10)
                                        .overlay(
                                            Circle()
                                                .stroke(AppColors.warning.opacity(0.5), lineWidth: 2)
                                                .scaleEffect(audioRecorder.isRecording ? 1.4 : 1.0)
                                                .opacity(audioRecorder.isRecording ? 0.0 : 1.0)
                                                .animation(.easeOut(duration: 1.5).repeatForever(autoreverses: false), value: audioRecorder.isRecording)
                                        )
                                }
                            } else {
                                Button(action: startRecording) {
                                    VStack(spacing: 8) {
                                        Image(systemName: "mic.fill")
                                            .font(.system(size: 32))
                                            .foregroundStyle(.white)
                                            .frame(width: 72, height: 72)
                                            .background(AppColors.secondary)
                                            .clipShape(Circle())
                                            .shadow(color: AppColors.secondary.opacity(0.4), radius: 10)
                                        
                                        Text("Tap to Record")
                                            .font(.subheadline)
                                            .foregroundStyle(.secondary)
                                    }
                                }
                            }
                        }
                        .padding()
                        .frame(maxWidth: .infinity)
                        .glassCard()
                    }
                }
                .padding(.horizontal)
            }
            .padding(.vertical)
        }
        .background(AppColors.background) // Pure White
        .navigationTitle(note.title?.isEmpty == false ? note.title! : note.createdAt.formatted(date: .abbreviated, time: .shortened))
        .navigationBarTitleDisplayMode(.inline)
        .glassEffect()
        .onDisappear {
            audioPlayer.stopPlayback()
            if audioRecorder.isRecording {
                let _ = audioRecorder.stopRecording()
            }
            
            // Clean up empty notes (no title, no audio)
            if (note.title == nil || note.title?.isEmpty == true) && note.audioPath == nil {
                modelContext.delete(note)
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(role: .destructive) {
                    showDeleteAlert = true
                } label: {
                    Image(systemName: "trash")
                        .foregroundStyle(AppColors.secondary)
                }
            }
        }
        .alert("Delete Note?", isPresented: $showDeleteAlert) {
            Button("Cancel", role: .cancel) { }
            Button("Delete", role: .destructive) {
                deleteNote()
            }
        } message: {
            Text("This action cannot be undone.")
        }
        .sheet(item: $shareItems) { items in
             ShareSheet(items: items.items)
                .presentationDetents([.medium, .large])
        }
    }
    
    // Wrapper for sheet items
    struct ShareItems: Identifiable {
        let id = UUID()
        let items: [Any]
    }
    
    @State private var shareItems: ShareItems?

    func showShareSheet(items: [Any]) {
        self.shareItems = ShareItems(items: items)
    }

    func getAudioUrl(for path: String) -> URL? {
        let fileManager = FileManager.default
        let appSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let audioDir = appSupport.appendingPathComponent("AudioNotes")
        return audioDir.appendingPathComponent(path)
    }
    
    @State private var showDeleteAlert = false
    @Environment(\.dismiss) private var dismiss
    
    // MARK: - Actions
    
    func deleteNote() {
        modelContext.delete(note)
        dismiss()
    }
    
    func startRecording() {
        Task {
            if await audioRecorder.requestPermission() {
                if audioRecorder.startRecording(noteID: note.id) != nil {
                    // Update state if needed, mostly handled by @Observable
                }
            }
        }
    }
    
    func stopRecording() {
        if let (url, duration) = audioRecorder.stopRecording() {
            note.audioPath = url.lastPathComponent
            note.durationMillis = Int(duration * 1000)
            note.status = .pending
            note.transcriptionID = UUID() // New job
            
            // Delegate to Coordinator (handles BG Task & Notifications)
            Task {
                await MainActor.run {
                    BackgroundTranscriptionCoordinator.shared.scheduleTranscription(for: note)
                }
            }
        }
    }
    
    func cancelTranscription() {
        TranscriptionService.shared.cancelCurrentTranscription()
        note.status = .cancelled
    }
    
    func formatTime(_ time: TimeInterval) -> String {
        let minutes = Int(time) / 60
        let seconds = Int(time) % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
    

}

struct ShareSheet: UIViewControllerRepresentable {
    var items: [Any]
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

#Preview {
    NavigationStack {
        NoteDetailScreen(note: Note(title: "Preview Note", transcript: "This is a preview transcript.", durationMillis: 120000))
    }
}
