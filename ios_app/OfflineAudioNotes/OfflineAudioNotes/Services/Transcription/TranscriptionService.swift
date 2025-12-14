import Foundation
import SwiftData

@Observable
class TranscriptionService {
    static let shared = TranscriptionService()
    
    // Bridge instance
    private var bridge: WhisperBridge?
    private var modelPath: String?
    
    private init() {}
    
    // Model Management
    func ensureModelLoaded() async throws {
        if bridge != nil { return }
        
        // 1. Check if model exists in App Support
        let fileManager = FileManager.default
        let appSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let modelsDir = appSupport.appendingPathComponent("models")
        let modelUrl = modelsDir.appendingPathComponent("ggml-tiny.bin")
        
        if !fileManager.fileExists(atPath: modelUrl.path) {
            // 2. Copy from Bundle if not in App Support
            guard let bundleUrl = Bundle.main.url(forResource: "ggml-tiny", withExtension: "bin") else {
                print("Error: ggml-tiny.bin not found in Bundle")
                throw TranscriptionError.modelNotFoundInBundle
            }
            
            try? fileManager.createDirectory(at: modelsDir, withIntermediateDirectories: true)
            try fileManager.copyItem(at: bundleUrl, to: modelUrl)
            print("Copied model to: \(modelUrl.path)")
        }
        
        // 3. Initialize Bridge
        guard let newBridge = WhisperBridge(modelPath: modelUrl.path) else {
            throw TranscriptionError.failedToInitializeBridge
        }
        
        self.bridge = newBridge
        self.modelPath = modelUrl.path
        print("Whisper Bridge Initialized")
    }
    
    // Transcription
    // Cancellation
    private var currentTask: Task<String?, Error>?
    
    func cancelCurrentTranscription() {
        currentTask?.cancel()
    }
    
    // Direct path transcription (for Coordinator)
    func transcribe(audioPath: String) async throws -> String {
        return try await transcribeInternal(audioPath: audioPath)
    }

    // Main Transcription Logic
    func transcribe(note: Note) async {
        guard let audioPath = note.audioPath else { return }
        
        do {
            // Update Status
            await MainActor.run {
                note.status = .processing
                note.lastTranscriptionError = nil
            }
            
            let transcript = try await transcribeInternal(audioPath: audioPath)
            
            // Update Note
            await MainActor.run {
                note.transcript = transcript
                note.status = .completed
            }
            
        } catch is CancellationError {
            await MainActor.run {
                note.status = .cancelled
                note.lastTranscriptionError = nil
            }
        } catch {
            print("Transcription failed: \(error)")
            await MainActor.run {
                note.status = .failed
                note.lastTranscriptionError = error.localizedDescription
            }
        }
    }
    
    private func transcribeInternal(audioPath: String) async throws -> String {
        try await ensureModelLoaded()
        
        guard let bridge = self.bridge else {
            throw TranscriptionError.bridgeNotReady
        }
        
        // Get full audio path
        let fileManager = FileManager.default
        let appSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let audioDir = appSupport.appendingPathComponent("AudioNotes")
        // Check if audioPath is already full path or relative
        let isAbsolutePath = audioPath.hasPrefix("/")
        let fullAudioUrl = isAbsolutePath ? URL(fileURLWithPath: audioPath) : audioDir.appendingPathComponent(audioPath)
        
        if !fileManager.fileExists(atPath: fullAudioUrl.path) {
             throw TranscriptionError.audioFileNotFound
        }
        
        print("Transcribing: \(fullAudioUrl.path)")
        
        // Wrap in cancellable task
        let task = Task.detached(priority: .userInitiated) {
            // Check cancellation before
            if Task.isCancelled { throw CancellationError() }
            
            let result = bridge.transcribeAudio(atPath: fullAudioUrl.path)
            
            // Check cancellation after (Soft Cancel)
            if Task.isCancelled { throw CancellationError() }
            
            return result
        }
        
        currentTask = task
        
        guard let transcript = try await task.value else {
            throw TranscriptionError.bridgeExecutionFailed
        }
        
        return transcript
    }
}

enum TranscriptionError: Error {
    case modelNotFoundInBundle
    case failedToInitializeBridge
    case bridgeNotReady
    case audioFileNotFound
    case bridgeExecutionFailed
}
