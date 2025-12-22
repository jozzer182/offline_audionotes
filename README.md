<div align="center">
  <img src="icons/iOS/Icon-180.png" width="120" alt="Offline Audio Notes Icon"/>
  <h1>🎙️ Offline Audio Notes</h1>
  <p><strong>A fully offline voice memo app with AI-powered transcription for Android &amp; iOS</strong></p>

  <p>
    <a href="#features"><img src="https://img.shields.io/badge/Features-✨-blue?style=for-the-badge" alt="Features"/></a>
    <a href="#demo"><img src="https://img.shields.io/badge/Demo-🎬-purple?style=for-the-badge" alt="Demo"/></a>
    <a href="#screenshots"><img src="https://img.shields.io/badge/Screenshots-📱-green?style=for-the-badge" alt="Screenshots"/></a>
    <a href="#architecture"><img src="https://img.shields.io/badge/Architecture-🏗️-orange?style=for-the-badge" alt="Architecture"/></a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android"/>
    <img src="https://img.shields.io/badge/iOS-000000?style=flat-square&logo=apple&logoColor=white" alt="iOS"/>
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"/>
    <img src="https://img.shields.io/badge/Swift-FA7343?style=flat-square&logo=swift&logoColor=white" alt="Swift"/>
    <img src="https://img.shields.io/badge/whisper.cpp-00ADD8?style=flat-square&logo=openai&logoColor=white" alt="whisper.cpp"/>
    <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="MIT License"/>
  </p>
</div>

---

## 📲 Download

<p align="center">
  <a href="https://apps.apple.com/us/app/offline-audio-notes/id6756551299">
    <img src="https://developer.apple.com/assets/elements/badges/download-on-the-app-store.svg" height="50" alt="Download on the App Store"/>
  </a>
  &nbsp;&nbsp;
  <img src="https://upload.wikimedia.org/wikipedia/commons/7/78/Google_Play_Store_badge_EN.svg" height="50" alt="Get it on Google Play" style="opacity: 0.5"/>
  <br/>
  <sub>Android version coming soon</sub>
</p>

---

<h2 id="features">✨ Features</h2>

<table>
  <tr>
    <td align="center" width="25%">
      <h3>🔒</h3>
      <strong>100% Offline</strong><br/>
      <sub>Your voice notes never leave your device. No internet required.</sub>
    </td>
    <td align="center" width="25%">
      <h3>🤖</h3>
      <strong>AI Transcription</strong><br/>
      <sub>Powered by <a href="https://github.com/ggerganov/whisper.cpp">whisper.cpp</a>, OpenAI's Whisper running locally.</sub>
    </td>
    <td align="center" width="25%">
      <h3>⚡</h3>
      <strong>Real-time Recording</strong><br/>
      <sub>Clean, modern UI for effortless recording.</sub>
    </td>
    <td align="center" width="25%">
      <h3>📝</h3>
      <strong>Auto Titles</strong><br/>
      <sub>Transcriptions automatically generate note titles.</sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="25%">
      <h3>🎨</h3>
      <strong>Modern Design</strong><br/>
      <sub>Material 3 (Android) &amp; Liquid Glass (iOS).</sub>
    </td>
    <td align="center" width="25%">
      <h3>📤</h3>
      <strong>Share Audio</strong><br/>
      <sub>Export and share recordings via any app.</sub>
    </td>
    <td align="center" width="25%">
      <h3>🌐</h3>
      <strong>Multi-language</strong><br/>
      <sub>Transcription supports 99+ languages.</sub>
    </td>
    <td align="center" width="25%">
      <h3>🔋</h3>
      <strong>Background Processing</strong><br/>
      <sub>Transcription works even when app is closed.</sub>
    </td>
  </tr>
</table>

---

<h2 id="demo">🎬 Demo</h2>

<div align="center">
  <img src="media/demo.gif" width="400" alt="App Demo"/>
  <p><sub>Recording and transcribing a voice note in real-time</sub></p>
</div>

---

<h2 id="screenshots">📱 Screenshots</h2>

<table>
  <tr>
    <th colspan="3" align="center">
      <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
    </th>
    <th colspan="3" align="center">
      <img src="https://img.shields.io/badge/iOS-000000?style=for-the-badge&logo=apple&logoColor=white" alt="iOS"/>
    </th>
  </tr>
  <tr>
    <td align="center"><img src="media/screenshots/android_1.png" width="180" alt="Android Notes List"/></td>
    <td align="center"><img src="media/screenshots/android_2.png" width="180" alt="Android Recording"/></td>
    <td align="center"><img src="media/screenshots/android_3.png" width="180" alt="Android Note Detail"/></td>
    <td align="center"><img src="media/screenshots/ios_1.png" width="180" alt="iOS Notes List"/></td>
    <td align="center"><img src="media/screenshots/ios_2.png" width="180" alt="iOS Recording"/></td>
    <td align="center"><img src="media/screenshots/ios_3.png" width="180" alt="iOS Note Detail"/></td>
  </tr>
</table>

---

<h2 id="architecture">🏗️ Architecture</h2>

```
offline_audionotes/
├── 📱 android_app/              # Android (Kotlin + Compose)
│   ├── app/src/main/
│   │   ├── cpp/                 # JNI bindings for whisper.cpp
│   │   ├── java/                # Kotlin source code
│   │   └── res/                 # Android resources
│   └── gradle/
├── 🍎 ios_app/                  # iOS (Swift + SwiftUI)
│   ├── OfflineAudioNotes/
│   │   ├── App/                 # App entry point
│   │   ├── Data/                # Repository implementations
│   │   ├── Domain/              # Models & Service interfaces
│   │   ├── Services/            # Audio, Transcription, Background
│   │   ├── UI/                  # Screens, Components, Theme
│   │   └── whisper.xcframework
│   └── ThirdParty/
├── 🤖 whisper.cpp/              # AI model (git submodule)
└── 📸 media/                    # Screenshots & demo GIF
```

<details>
<summary><strong>📦 Key Components - Android</strong></summary>
<br/>

| Component | Description |
|-----------|-------------|
| **MainActivity** | Note list with RecyclerView |
| **NoteDetailActivity** | View/edit notes with audio playback |
| **WavyAudioPlayer** | Jetpack Compose audio player with Material 3 Expressive |
| **TranscribeNoteWorker** | Background transcription using WorkManager |
| **WhisperBridge** | JNI bridge to whisper.cpp native library |
| **Room Database** | Local persistence for notes |

</details>

<details>
<summary><strong>📦 Key Components - iOS</strong></summary>
<br/>

| Component | Description |
|-----------|-------------|
| **NoteListView** | Main screen with SwiftUI List |
| **NoteDetailView** | View/edit notes with audio playback |
| **AudioPlayerView** | SwiftUI audio player component |
| **BackgroundTranscriptionCoordinator** | Background using BGProcessingTask |
| **WhisperBridge** | Swift bridge to whisper.xcframework |
| **SwiftData** | Local persistence for notes |

</details>

---

<h2 id="tech-stack">🛠️ Tech Stack</h2>

<table>
  <tr>
    <th align="center">
      <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
    </th>
    <th align="center">
      <img src="https://img.shields.io/badge/iOS-000000?style=for-the-badge&logo=apple&logoColor=white" alt="iOS"/>
    </th>
    <th align="center">
      <img src="https://img.shields.io/badge/Native-00ADD8?style=for-the-badge&logo=c&logoColor=white" alt="Native"/>
    </th>
  </tr>
  <tr>
    <td valign="top">
      <ul>
        <li><strong>Kotlin 2.0</strong></li>
        <li>Min SDK 26 (Android 8.0)</li>
        <li>Target SDK 36 (Android 16)</li>
        <li>Jetpack Compose</li>
        <li>Material 3 Expressive</li>
        <li>Room Database</li>
        <li>WorkManager</li>
        <li>Coroutines</li>
      </ul>
    </td>
    <td valign="top">
      <ul>
        <li><strong>Swift 5.10</strong></li>
        <li>iOS 17.0+</li>
        <li>SwiftUI</li>
        <li>SwiftData</li>
        <li>AVFoundation</li>
        <li>BackgroundTasks</li>
        <li>Combine</li>
      </ul>
    </td>
    <td valign="top">
      <ul>
        <li><strong>whisper.cpp</strong></li>
        <li>CMake</li>
        <li>JNI (Android)</li>
        <li>Swift C Interop (iOS)</li>
      </ul>
    </td>
  </tr>
</table>

---

<h2 id="setup">🚀 Setup</h2>

### Prerequisites

<table>
  <tr>
    <td>
      <img src="https://img.shields.io/badge/Android_Studio-Ladybug_2024.2.1+-3DDC84?style=flat-square&logo=android-studio&logoColor=white" alt="Android Studio"/>
    </td>
    <td>
      <img src="https://img.shields.io/badge/Xcode-15+-147EFB?style=flat-square&logo=xcode&logoColor=white" alt="Xcode"/>
    </td>
  </tr>
  <tr>
    <td>
      • Android SDK 36<br/>
      • NDK 27.x<br/>
      • CMake 3.22.1+
    </td>
    <td>
      • iOS 17.0+ SDK<br/>
      • Swift 5.10
    </td>
  </tr>
</table>

### Clone & Build

```bash
# Clone with submodules
git clone --recursive https://github.com/jozzer182/offline_audionotes.git
cd offline_audionotes

# Download Whisper model (75MB)
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin
```

<details>
<summary><strong>🤖 Build for Android</strong></summary>

1. Open `android_app/` in Android Studio
2. Sync Gradle
3. Build and run on device/emulator (min SDK 26)

</details>

<details>
<summary><strong>🍎 Build for iOS</strong></summary>

1. Open `ios_app/OfflineAudioNotes.xcodeproj` in Xcode
2. Run `git submodule update --init --recursive`
3. Ensure `whisper.xcframework` is linked in Build Phases
4. Build and run on iPhone/Simulator (iOS 17+)

</details>

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

```bash
# 1. Fork the repository
# 2. Create your feature branch
git checkout -b feature/AmazingFeature

# 3. Commit your changes
git commit -m 'Add some AmazingFeature'

# 4. Push to the branch
git push origin feature/AmazingFeature

# 5. Open a Pull Request
```

---

## 📄 License

<table>
  <tr>
    <td>
      <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="MIT License"/>
    </td>
    <td>
      This project is licensed under the MIT License - see the <a href="LICENSE">LICENSE</a> file for details.
    </td>
  </tr>
</table>

### Third-Party Licenses

- **whisper.cpp** - MIT License - [ggerganov/whisper.cpp](https://github.com/ggerganov/whisper.cpp)
- **Whisper Model** - MIT License - [OpenAI Whisper](https://github.com/openai/whisper)

---

## 🙏 Acknowledgments

<table>
  <tr>
    <td align="center">
      <a href="https://openai.com">
        <img src="https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white" alt="OpenAI"/>
      </a>
      <br/><sub>Whisper Model</sub>
    </td>
    <td align="center">
      <a href="https://github.com/ggerganov">
        <img src="https://img.shields.io/badge/ggerganov-100000?style=for-the-badge&logo=github&logoColor=white" alt="ggerganov"/>
      </a>
      <br/><sub>whisper.cpp</sub>
    </td>
  </tr>
</table>

---

<div align="center">
  <p>
    <strong>Made with ❤️ for privacy-conscious users</strong>
  </p>
  <p>
    <a href="https://github.com/jozzer182/offline_audionotes/stargazers">
      <img src="https://img.shields.io/github/stars/jozzer182/offline_audionotes?style=social" alt="Stars"/>
    </a>
    <a href="https://github.com/jozzer182/offline_audionotes/fork">
      <img src="https://img.shields.io/github/forks/jozzer182/offline_audionotes?style=social" alt="Forks"/>
    </a>
  </p>
</div>
