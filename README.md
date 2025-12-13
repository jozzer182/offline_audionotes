# 🎙️ Offline Audio Notes

<p align="center">
  <img src="android_app/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" width="120" alt="App Icon"/>
</p>

<p align="center">
  <strong>A fully offline voice memo app with AI-powered transcription</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#setup">Setup</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#license">License</a>
</p>

---

## ✨ Features

- **🔒 100% Offline** - No internet required. Your voice notes never leave your device.
- **🤖 AI Transcription** - Powered by [whisper.cpp](https://github.com/ggerganov/whisper.cpp), OpenAI's Whisper model running locally.
- **⚡ Real-time Recording** - Clean, modern UI for recording voice memos.
- **📝 Auto-generated Titles** - Transcriptions automatically generate note titles.
- **🎨 Material 3 Design** - Modern Android design with Material You theming.
- **📤 Share Audio** - Export and share your recordings via any app.
- **🌐 Multi-language** - Transcription supports multiple languages.

---

## 📱 Screenshots

<p align="center">
  <i>Screenshots coming soon</i>
</p>

---

## 🏗️ Architecture

```
offline_audionotes/
├── android_app/          # Main Android application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── cpp/      # JNI bindings for whisper.cpp
│   │   │   ├── java/     # Kotlin source code
│   │   │   └── res/      # Android resources
│   │   └── build.gradle.kts
│   └── gradle/
├── whisper.cpp/          # Whisper.cpp library (git submodule)
└── README.md
```

### Key Components

| Component | Description |
|-----------|-------------|
| **MainActivity** | Note list with RecyclerView |
| **NoteDetailActivity** | View/edit notes with audio playback |
| **WavyAudioPlayer** | Jetpack Compose audio player with Material 3 Expressive |
| **TranscribeNoteWorker** | Background transcription using WorkManager |
| **WhisperBridge** | JNI bridge to whisper.cpp native library |
| **Room Database** | Local persistence for notes |

---

## 🚀 Setup

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- Android SDK 36 (Android 16)
- NDK 27.x
- CMake 3.22.1+

### Clone the Repository

```bash
git clone --recursive https://github.com/YOUR_USERNAME/offline-audionotes.git
cd offline-audionotes
```

> **Note:** The `--recursive` flag is important to clone the whisper.cpp submodule.

### Download Whisper Model

The app uses the `ggml-tiny.bin` model. You can download it automatically on first run, or manually:

```bash
# Download tiny model (75MB)
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin
```

Place the model in the app's files directory or let the app download it.

### Build

1. Open `android_app/` in Android Studio
2. Sync Gradle
3. Build and run on device/emulator (min SDK 26)

---

## 🛠️ Tech Stack

### Android
- **Language**: Kotlin 2.0
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36 (Android 16)
- **UI**: XML Views + Jetpack Compose (hybrid)
- **Architecture**: Single-activity with ViewModel

### Libraries

| Library | Purpose |
|---------|---------|
| Jetpack Compose | Modern UI components (audio player) |
| Material 3 | Design system with Expressive APIs |
| Room | Local database |
| WorkManager | Background transcription |
| Coroutines | Asynchronous programming |

### Native

| Library | Purpose |
|---------|---------|
| whisper.cpp | On-device speech recognition |
| CMake | Native build system |
| JNI | Java-Native Interface bindings |

---

## 📂 Project Structure

```
android_app/app/src/main/
├── cpp/
│   ├── CMakeLists.txt          # Native build configuration
│   └── whisper_jni.cpp         # JNI bindings
├── java/.../offlineaudionotes/
│   ├── data/
│   │   └── local/              # Room database & DAOs
│   ├── domain/
│   │   └── model/              # Data models
│   ├── stt/
│   │   ├── WhisperBridge.kt    # Native library interface
│   │   └── WhisperModelManager.kt
│   ├── ui/
│   │   ├── components/         # Compose components
│   │   ├── MainActivity.kt
│   │   └── NoteDetailActivity.kt
│   └── worker/
│       └── TranscribeNoteWorker.kt
└── res/
    ├── layout/                 # XML layouts
    └── drawable/               # Icons and graphics
```

---

## 🔧 Configuration

### Foreground Service (Android 14+)

The app uses a foreground service for transcription. For Android 14+ compatibility, the service declares `foregroundServiceType="dataSync"`.

### Permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Third-Party Licenses

- **whisper.cpp** - MIT License - [ggerganov/whisper.cpp](https://github.com/ggerganov/whisper.cpp)
- **Whisper Model** - MIT License - [OpenAI Whisper](https://github.com/openai/whisper)

---

## 🙏 Acknowledgments

- [OpenAI](https://openai.com) for the Whisper speech recognition model
- [Georgi Gerganov](https://github.com/ggerganov) for whisper.cpp
- The Android community for their amazing libraries

---

<p align="center">
  Made with ❤️ for privacy-conscious users
</p>
