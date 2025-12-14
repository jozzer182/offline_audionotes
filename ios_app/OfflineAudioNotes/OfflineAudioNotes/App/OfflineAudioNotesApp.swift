//
//  OfflineAudioNotesApp.swift
//  OfflineAudioNotes
//
//  Created by JOSE ZARABANDA on 12/13/25.
//

import SwiftData
import SwiftUI

@main
struct OfflineAudioNotesApp: App {

    init() {
        // Register background task immediately on startup
        BackgroundTranscriptionCoordinator.shared.registerBackgroundTask()
    }
    
    var body: some Scene {
        WindowGroup {
            NotesListScreen()
                .onAppear {
                    BackgroundTranscriptionCoordinator.shared.requestPermissions()
                }
        }
        .modelContainer(for: Note.self)
    }
}
