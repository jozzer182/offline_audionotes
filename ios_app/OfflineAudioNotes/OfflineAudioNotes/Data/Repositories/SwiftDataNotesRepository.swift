//
//  SwiftDataNotesRepository.swift
//  OfflineAudioNotes
//
//  Created by JOSE ZARABANDA on 12/13/25.
//

import Foundation
import SwiftData

@MainActor
class SwiftDataNotesRepository {
    private let modelContext: ModelContext
    
    init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }
    
    func addNote(_ note: Note) {
        modelContext.insert(note)
    }
    
    func deleteNote(_ note: Note) {
        modelContext.delete(note)
    }
    
    func save() throws {
        try modelContext.save()
    }
}
