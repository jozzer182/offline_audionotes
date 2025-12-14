import Foundation
import BackgroundTasks
import UserNotifications
import SwiftData
import UIKit

@MainActor
class BackgroundTranscriptionCoordinator: NSObject, UNUserNotificationCenterDelegate {
    static let shared = BackgroundTranscriptionCoordinator()
    
    private let backgroundTaskID = "com.josezarabanda.OfflineAudioNotes.transcribe"
    private let modelContainer: ModelContainer
    
    private override init() {
        do {
            modelContainer = try ModelContainer(for: Note.self)
        } catch {
            fatalError("Failed to create ModelContainer: \(error)")
        }
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }
    
    func requestPermissions() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if let error = error {
                print("Notification permission error: \(error)")
            }
        }
    }
    
    func registerBackgroundTask() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: backgroundTaskID, using: nil) { task in
            if let processingTask = task as? BGProcessingTask {
                self.handleBackgroundTask(processingTask)
            } else {
                task.setTaskCompleted(success: false)
            }
        }
    }
    
    func scheduleTranscription(for note: Note) {
        // 1. Send Notification (Immediate Feedback)
        sendNotification(title: "Transcribing...", body: "Processing your note in the background.")
        
        // 2. Start Immediate Execution wrapped in Background Task
        Task {
            var bgTaskID: UIBackgroundTaskIdentifier = .invalid
            bgTaskID = UIApplication.shared.beginBackgroundTask {
                // Expiration Handler: End the task if time runs out
                UIApplication.shared.endBackgroundTask(bgTaskID)
                bgTaskID = .invalid
            }
            
            // Perform Transcription
            await TranscriptionService.shared.transcribe(note: note)
            
            // End Background Task
            UIApplication.shared.endBackgroundTask(bgTaskID)
            bgTaskID = .invalid
            
            // 3. Notify Completion
            if note.status == .completed {
                sendNotification(title: "Transcription Ready", body: "Your note has been transcribed.", noteID: note.id.uuidString)
            } else if note.status == .failed {
                sendNotification(title: "Transcription Failed", body: "Could not transcribe audio.")
            }
        }
        
        // 4. Schedule Retry via BGTaskScheduler (Best Effort)
        // This is primarily for if the app is killed or fails significantly, 
        // though strictly speaking we'd need to persisting the 'pending' state to pick it up later.
        let request = BGProcessingTaskRequest(identifier: backgroundTaskID)
        request.requiresNetworkConnectivity = false
        request.requiresExternalPower = false
        
        do {
            try BGTaskScheduler.shared.submit(request)
            print("[BGTask] Scheduled retry task for note: \(note.id)")
        } catch {
            // Note: Error Code 1 is expected on Simulator
            print("[BGTask] Scheduling failed (expected on Simulator): \(error)")
        }
    }
    
    private func handleBackgroundTask(_ task: BGProcessingTask) {
        // Create context
        let context = ModelContext(modelContainer)
        
        // Find pending notes
        // Fetch all notes and filter in memory (Workaround for Predicate Enum issue)
        let descriptor = FetchDescriptor<Note>()
        
        guard let allNotes = try? context.fetch(descriptor) else {
            task.setTaskCompleted(success: true)
            return
        }
        
        let notes = allNotes.filter { $0.status == .pending || $0.status == .processing }
        
        if notes.isEmpty {
            task.setTaskCompleted(success: true)
            return
        }
        
        // Expiration handler
        task.expirationHandler = {
            // Cancel current work
            print("Background task expired")
            // In a real implementation, we'd signal TranscriptionService to cancel
            // For now, we just mark task complete.
        }
        
        // Process first pending note
        if let note = notes.first, let audioPath = note.audioPath {
            Task {
                do {
                    // Update status
                    note.status = .processing
                    try context.save()
                    
                    let result = try await TranscriptionService.shared.transcribe(audioPath: audioPath)
                    
                    note.transcript = result
                    note.status = .completed
                    try context.save()
                    
                    self.sendNotification(title: "Transcription ready", body: "Your note is now transcribed.", noteID: note.id.uuidString)
                    task.setTaskCompleted(success: true)
                } catch {
                    note.status = .failed
                    note.lastTranscriptionError = error.localizedDescription
                    try? context.save()
                    
                    self.sendNotification(title: "Transcription failed", body: "Tap to retry.")
                    task.setTaskCompleted(success: false)
                }
            }
        } else {
            task.setTaskCompleted(success: true)
        }
    }
    
    func sendNotification(title: String, body: String, noteID: String? = nil) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        
        if let noteID = noteID {
            content.userInfo = ["noteID": noteID]
        }
        
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
    
    // MARK: - UNUserNotificationCenterDelegate
    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound])
    }
    
    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        // Handle deep link if needed (can be passed to App via SceneDelegate/App)
        // For SwiftUI, we might store this in a Published property or AppStorage used by NavigationStack
        completionHandler()
    }
}
