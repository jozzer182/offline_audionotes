import Foundation
import AVFoundation

@Observable
class AudioRecorder: NSObject, AVAudioRecorderDelegate {
    var isRecording = false
    var recordingTime: TimeInterval = 0
    private var audioRecorder: AVAudioRecorder?
    private var timer: Timer?
    
    // Path to the current recording
    var currentRecordingURL: URL?
    
    // Directory to store recordings
    private let recordingsDirectory: URL = {
        let paths = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)
        let directory = paths[0].appendingPathComponent("AudioNotes")
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }()
    
    override init() {
        super.init()
        setupAudioSession()
    }
    
    private func setupAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .default)
            try session.setActive(true)
        } catch {
            print("Failed to set up audio session: \(error)")
        }
    }
    
    func requestPermission() async -> Bool {
        await withCheckedContinuation { continuation in
            AVAudioApplication.requestRecordPermission { granted in
                continuation.resume(returning: granted)
            }
        }
    }
    
    func startRecording(noteID: UUID) -> URL? {
        let fileName = "note_\(noteID.uuidString)_\(Int(Date().timeIntervalSince1970)).wav"
        let fileURL = recordingsDirectory.appendingPathComponent(fileName)
        
        // Settings for WAV 16kHz Mono (Whisper friendly behavior - though we just do standard PCM here)
        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatLinearPCM),
            AVSampleRateKey: 16000.0,
            AVNumberOfChannelsKey: 1,
            AVLinearPCMBitDepthKey: 16,
            AVLinearPCMIsBigEndianKey: false,
            AVLinearPCMIsFloatKey: false
        ]
        
        do {
            audioRecorder = try AVAudioRecorder(url: fileURL, settings: settings)
            audioRecorder?.delegate = self
            audioRecorder?.record()
            
            isRecording = true
            currentRecordingURL = fileURL
            startTimer()
            print("Started recording to: \(fileURL.path)")
            return fileURL
        } catch {
            print("Could not start recording: \(error)")
            return nil
        }
    }
    
    func stopRecording() -> (URL, TimeInterval)? {
        guard let url = currentRecordingURL else { return nil }
        let duration = recordingTime
        
        audioRecorder?.stop()
        audioRecorder = nil
        isRecording = false
        stopTimer()
        
        return (url, duration)
    }
    
    private func startTimer() {
        recordingTime = 0
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            self.recordingTime += 0.1
        }
    }
    
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
        // Resetting recording time is handled on start
    }
    
    // AVAudioRecorderDelegate
    func audioRecorderDidFinishRecording(_ recorder: AVAudioRecorder, successfully flag: Bool) {
        if !flag {
            // Handle failure
            print("Recording failed")
        }
        isRecording = false
        stopTimer()
    }
}
