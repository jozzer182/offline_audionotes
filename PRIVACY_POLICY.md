# Privacy Policy

**Offline Audio Notes**  
*Last updated: December 2024*

---

## Overview

Offline Audio Notes is designed with **privacy as a core principle**. The app operates entirely offline and does not collect, transmit, or share any user data.

---

## Data Collection

### What We DON'T Collect

- ❌ Personal information
- ❌ Audio recordings
- ❌ Transcriptions
- ❌ Usage analytics
- ❌ Device identifiers
- ❌ Location data
- ❌ Advertising data

### What Stays on Your Device

All data created by the app remains **exclusively on your device**:

| Data Type | Storage Location | Shared? |
|-----------|------------------|---------|
| Voice recordings | App's private storage | No |
| Transcriptions | Local SQLite database | No |
| App settings | SharedPreferences | No |
| AI Model | App's files directory | No |

---

## Offline Operation

This app is designed to work **100% offline**:

- **No internet required** - All features work without connectivity
- **No cloud sync** - Your notes never leave your device
- **No accounts** - No registration or login required
- **Local AI** - Speech recognition runs entirely on your device using [whisper.cpp](https://github.com/ggerganov/whisper.cpp)

---

## Permissions

The app requests the following permissions:

| Permission | Purpose | Required? |
|------------|---------|-----------|
| `RECORD_AUDIO` | Record voice memos | Yes |
| `POST_NOTIFICATIONS` | Show transcription progress | Optional |
| `FOREGROUND_SERVICE` | Background transcription | Yes |

We only request permissions necessary for core functionality.

---

## Data Sharing

**We do not share any data with third parties.**

- No advertising networks
- No analytics services
- No cloud services
- No social media integrations

---

## Data Security

Your data is protected by:

1. **Device encryption** - Relies on your device's built-in encryption
2. **Private storage** - Files stored in app-private directories
3. **No network access** - App has no internet permission

---

## Children's Privacy

This app does not knowingly collect information from children under 13. Since no data is collected at all, this app is safe for users of all ages.

---

## Changes to This Policy

We may update this Privacy Policy from time to time. Changes will be reflected in the "Last updated" date above.

---

## Open Source

This app is open source. You can review the complete source code to verify our privacy practices:

🔗 [GitHub Repository](https://github.com/YOUR_USERNAME/offline-audionotes)

---

## Contact

If you have questions about this Privacy Policy, please open an issue on GitHub.

---

<p align="center">
  <strong>Your voice, your notes, your device. Always private.</strong>
</p>
