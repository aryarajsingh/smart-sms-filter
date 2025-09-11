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
- **Framework**: Spam and Call protection service (not default SMS app)
- **UI**: Kotlin + Jetpack Compose
- **Architecture**: MVVM with Clean Architecture + Room Database
- **AI**: TensorFlow Lite for on-device classification
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
- ✅ Android SMS filtering service implementation
- ✅ Rule-based classification with user preferences integration
- ✅ Three-category system (Inbox, Filtered, Needs Review)
- ✅ Premium Welcome Screen with value proposition
- ✅ Enhanced Onboarding Flow with user preference collection
- ✅ Premium iOS-inspired UI with dynamic theming
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
- Fixed MainActivity smart casting and WelcomeScreen import issues
- Enhanced onboarding with progress headers and better UX
- Integrated user preferences into classification logic
- Replaced inappropriate "iMessage" placeholder text
- Added premium Welcome Screen with spring animations

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
