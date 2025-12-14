//
//  NotesListScreen.swift
//  OfflineAudioNotes
//
//  Created by JOSE ZARABANDA on 12/13/25.
//

import SwiftUI

import SwiftData

struct NotesListScreen: View {
    @State private var searchText = ""
    @State private var path = NavigationPath() // Programmatic Navigation
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        NavigationStack(path: $path) { // Bind path
            ZStack {
                AppColors.background
                    .ignoresSafeArea()
                
                NotesListView(searchText: searchText)
            }
            .navigationTitle("Audio Notes")
            .navigationDestination(for: Note.self) { note in
                NoteDetailScreen(note: note)
            }
            .searchable(text: $searchText, placement: .navigationBarDrawer(displayMode: .always))
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: createNote) {
                        Image(systemName: "plus")
                            .font(.headline)
                            .foregroundStyle(AppColors.primary) // Tinted icon
                            .padding(8)
                            .glassButton() // Liquid Glass button
                    }
                }
            }
        }
    }
    
    private func createNote() {
        let newNote = Note()
        modelContext.insert(newNote)
        // Auto-navigate to the new note
        path.append(newNote)
    }
}

struct NotesListView: View {
    @Query private var notes: [Note]
    let searchText: String
    
    init(searchText: String) {
        self.searchText = searchText
        if searchText.isEmpty {
            _notes = Query(sort: \Note.createdAt, order: .reverse)
        } else {
            _notes = Query(filter: #Predicate { note in
                (note.title?.localizedStandardContains(searchText) == true) ||
                (note.transcript?.localizedStandardContains(searchText) == true)
            }, sort: \Note.createdAt, order: .reverse)
        }
    }
    
    var body: some View {
        if notes.isEmpty && searchText.isEmpty {
            VStack {
                Image(systemName: "note.text")
                    .font(.system(size: 60))
                    .foregroundStyle(AppColors.secondary)
                    .padding()
                Text("No notes yet")
                    .foregroundStyle(.secondary)
            }
        } else if notes.isEmpty && !searchText.isEmpty {
            ContentUnavailableView.search(text: searchText)
        } else {
            ScrollView {
                LazyVStack(spacing: 16) {
                    ForEach(Array(notes.enumerated()), id: \.element.id) { index, note in
                        NavigationLink(value: note) { // Use value-based navigation
                            NoteRow(note: note)
                        }
                        .buttonStyle(PlainButtonStyle())
                        .transition(.scale(scale: 0.9).combined(with: .opacity))
                        .onAppear {
                            // Logic to animate if needed, but transition mainly works on insert
                        }
                    }
                }
                .animation(.spring(response: 0.5, dampingFraction: 0.7), value: notes)
                .padding()
            }
        }
    }
}

struct NoteRow: View {
    let note: Note
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let title = note.title, !title.isEmpty {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(.primary)
            }
            
            if let transcript = note.transcript, !transcript.isEmpty {
                Text(transcript)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(note.title?.isEmpty == false ? 2 : 4) // Show more lines if no title
            } else {
                Text("No transcript")
                    .font(.caption)
                    .foregroundStyle(AppColors.primary)
            }
            
            HStack {
                if note.status == .completed {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(AppColors.success)
                        .font(.caption)
                    Text("Transcribed")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                } else {
                    Image(systemName: "clock.fill")
                        .foregroundStyle(AppColors.highlight)
                        .font(.caption)
                    Text(note.status.rawValue.capitalized)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                
                Spacer()
                
                Text(note.createdAt.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding()
        .glassCard() // Liquid Glass Card
    }
}

#Preview {
    NotesListScreen()
        .modelContainer(for: Note.self, inMemory: true)
}
