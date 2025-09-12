# Smart SMS Filter

**Version 1.1.0** - An intelligent SMS filtering application for Android

## Overview

Smart SMS Filter is designed to give every smartphone user in India a clean, quiet, and secure messaging experience through intelligent, automated filtering. Using powerful on-device AI models, the app learns user preferences to automatically categorize and filter messages, separating important communications from promotional spam.

## The Problem We Solve

The average smartphone user in India receives a high volume of unsolicited SMS messages, ranging from promotions to scams. This digital noise buries essential communications like transaction alerts, OTPs, and personal messages, creating a frustrating and potentially insecure user experience.

## Core Features (MVP)

### 🚀 Quick Onboarding
- Set up in under 30 seconds with minimal permissions
- Immediate protection upon installation

### 🧠 Smart Filtering
- Automatic identification of promotional and spam messages
- Silent notifications for non-essential messages
- Priority alerts for important communications (OTPs, bank transactions)

### 🔍 Safety Net
- "Needs Review" section for uncertain messages
- Quick review without cluttering main inbox

### 📚 Learning System
- One-tap correction for mis-filtered messages
- Continuous improvement through user feedback

## Technical Architecture

### Android Implementation
- **SMS Role**: Default SMS app (Android 4.4+; RoleManager on Android 10+) for real-time deliver/processing
- **UI**: Kotlin + Jetpack Compose
- **Architecture**: Clean Architecture (MVVM) + Room Database
- **Notifications**: Important vs. normal vs. silent channels; OTPs are never silent
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)

### AI Model Specifications
- **Model**: Gemma 2B (fine-tuned and distilled)
- **Quantization**: 4-bit integer quantization
- **Framework**: TensorFlow Lite
- **Training**: Synthetic dataset via LLM + public datasets
- **Size**: ~50MB optimized for mobile

## Project Structure

```
smart-sms-filter/
├── android/           # Android app (Kotlin + Jetpack Compose)
├── models/           # TensorFlow Lite model files and training scripts
├── docs/             # Documentation and specifications
├── scripts/          # Build and deployment scripts
└── README.md         # This file
```

## Development Setup

### Prerequisites
- **Android Studio**: Hedgehog (2023.1.1) or newer
- **Kotlin**: 1.9+
- **Android SDK**: API 34 (with minimum API 24 support)
- **Java**: JDK 11 or newer
- **AI Models**: TensorFlow Lite
- **Version Control**: Git

### Quick Start

1. Clone the repository
2. Open `android/` directory in Android Studio
3. Sync Gradle files and dependencies
4. Add TensorFlow Lite model to `android/app/src/main/assets/`
5. Run on Android device with API 24+

## Target Audience

### Primary Users
- **Busy Professionals**: Need a "set and forget" solution with intelligent background filtering
- **Security-Conscious Users**: Want protection from scam messages with straightforward operation

## Roadmap

### Version 1.1.0 (Current - Android)
- ✅ Android SMS filtering service implementation (default SMS role support and banner)
- ✅ Rule-based classification with user preferences integration
- ✅ Three-category system (Inbox, Spam/Promo, Needs Review)
- ✅ Premium Welcome Screen with value proposition
- ✅ Premium Onboarding (legacy onboarding removed)
- ✅ Settings screen (Theme mode, Filtering strength, Important types, Learning toggle)
- ✅ Explainability: “Why?” bottom sheet with classification audit reasons
- ✅ Notifications refined; OTPs are never silent
- ✅ Jetpack Compose modern UI with refined typography and spacing
- 🚧 TensorFlow Lite AI model (architecture ready, not yet integrated)

### Future Versions
- iOS implementation (when development resources available)
- Cloud backup and sync
- Custom user rules
- MMS filtering
- Advanced analytics

## Contributing

Please read our contributing guidelines in `docs/CONTRIBUTING.md` before submitting pull requests.

## Privacy & Security

- **100% On-Device Processing**: No messages sent to external servers
- **Local AI Models**: All filtering happens locally
- **Minimal Permissions**: Only necessary SMS access permissions requested

## Current Development Context (v1.1.0)

This project is actively developed with AI assistance (Warp/Claude). Key context for development sessions:

### Current State
- **Filtering**: Rule-based classifier with user preference integration (NOT AI model yet)
- **UI**: Premium iOS-inspired design system with dynamic colors and refined typography
- **Build**: Successfully builds and runs on Android device
- **Version**: 1.1.0 (versionCode: 2)

### Recent Achievements
- Settings screen with Theme mode and filtering preferences (persisted via DataStore)
- Explainability “Why?” bottom sheet powered by classification audit table
- Refined notification policy: OTPs always high-priority (never silent)
- Default SMS app role detection and user prompt flow improvements
- Premium Welcome Screen and Onboarding polish (legacy onboarding removed)

### Next Steps
- Continue UI polish: bottom tab navigation, translucency effects
- Test current implementation thoroughly
- Future: AI model integration (Gemma 2B architecture documented)

### Known Issues
- Learn More button in WelcomeScreen needs implementation (TODO added)
- AI model integration pending (architecture ready, not connected)

### Development Workflow
```bash
# Version updates in app/build.gradle
versionCode = 2
versionName = "1.1.0"

# Git workflow
git add .
git commit -m "feat: description"
git tag v1.1.0
git push origin main --tags
```

## License

[License information to be added]

---

**Built with ❤️ for a cleaner, safer messaging experience in India**
