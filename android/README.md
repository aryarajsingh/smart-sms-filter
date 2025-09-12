# Smart SMS Filter - Android App

## Overview

Smart SMS Filter is a privacy-first SMS inbox. It organizes messages into Inbox, Spam, and Needs Review — on-device, in real time. It becomes the default SMS app (Android 4.4+) or requests the SMS role via RoleManager (Android 10+) to enable classification, notifications, and full functionality.

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room
- **Dependency Injection**: Hilt
- **AI**: TensorFlow Lite
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)

## Project Structure

```
android/
├── app/
│   ├── src/main/java/com/smartsmsfilter/
│   │   ├── ui/           # Compose UI components
│   │   ├── data/         # Repository, database, data sources
│   │   ├── domain/       # Use cases, entities
│   │   ├── presentation/ # ViewModels and UI state
│   │   └── ml/           # TensorFlow Lite integration
│   └── build.gradle      # App dependencies
├── gradle/               # Gradle wrapper files
└── build.gradle          # Project-level configuration
```

## Key Features

### Welcome and onboarding
- Calm, explanatory welcome: learn what the app does before any prompts
- Privacy pledge: all processing on-device; no data leaves your phone
- Request permissions and default SMS only after rationale

### SMS Filtering Service
- Real-time SMS classification using rule-based + contextual models (TFLite planned)
- Default SMS app requirement (Android 4.4+; RoleManager on Android 10+)
- Privacy-first: on-device processing

### Three-Category System
1. **Inbox**: Important messages (OTPs, banking, personal; trusted senders pinned to Inbox)
2. **Filtered (Spam/Promo)**: Promotional and suspected spam (delivered silently)
3. **Needs Review**: Uncertain classifications for user review

### Explainability and corrections
- "Why?" dialog with meaningful reasons (manual overrides, sender prefs, OTP rule, contextual hints)
- Correction actions (Move to Inbox / Mark Spam) with quick reason chips; undo supported
- Feedback audit logged for future improvements

### On-Device AI
- Gemma 2B model (4-bit quantized)
- 100% local processing - no data sent to servers
- Continuous learning from user feedback

## UI System

- Premium composer bar: rounded input, accessible send button
- Subtle list micro-animations (fade/scale) for item changes
- AutoMirrored icons for RTL support and to avoid deprecated icons
- Normalized paddings/typography for a calm, consistent feel

## Development Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Kotlin 1.9+
- Android SDK API 34
- Gradle 8.0+

### Setup Steps
1. Open Android Studio
2. Import this directory as an Android project
3. Sync Gradle files
4. Add TFLite model to `app/src/main/assets/`
5. Run on device with API 24+

## Permissions & Roles

The app requests permissions and default SMS role only after explaining why. Essentials include:
- RECEIVE_SMS, READ_SMS, SEND_SMS
- READ_CONTACTS
- POST_NOTIFICATIONS (Android 13+)
- Default SMS role via RoleManager (Android 10+) or Telephony API (Android 4.4–9)

## Architecture Details

### MVVM + Clean Architecture
- **UI Layer**: Jetpack Compose screens
- **Presentation Layer**: ViewModels managing UI state
- **Domain Layer**: Use cases and business logic
- **Data Layer**: Repository pattern with Room database

### Key Components
- `DefaultSmsAppHelper`: Handles default SMS app role detection/prompt
- `SmartNotificationManager`: Channel routing (important/normal/silent); OTPs never silent
- `ClassificationAuditDao`: Persists classification reasons for explainability
- `MessageClassifier`: (planned TFLite wrapper)
- `MessageRepository`: Data access layer
- `FilterViewModel`/`SmsViewModel`: UI state management

## Build Variants

### Debug
- Full logging enabled
- Development ML model
- Network security config for testing

### Release
- Optimized with R8/ProGuard
- Production ML model
- Security hardening enabled

## Testing Strategy

### Unit Tests
- ViewModel logic testing
- Use case testing
- ML model output validation

### Integration Tests
- Database operations
- Service functionality
- Permission handling

### UI Tests
- Compose UI testing
- User interaction flows
- Accessibility testing

## Model Integration

The TensorFlow Lite model should be placed in:
```
app/src/main/assets/sms_classifier_model.tflite
```

Model specifications:
- Input: Tokenized SMS text (max 512 tokens)
- Output: Classification probabilities [spam, important, uncertain]
- Size: ~50MB (4-bit quantized)

## Performance Considerations

- Model inference: <100ms on average
- Memory usage: <200MB including model
- Battery impact: Minimal (optimized for background operation)
- Storage: App size ~25MB + model ~50MB

## Security Features

- All processing happens on-device
- No network access for core filtering
- Encrypted local database
- Secure key storage for user preferences

## Contributing

1. Follow Kotlin coding standards
2. Use Jetpack Compose for all UI
3. Write tests for new features
4. Update documentation

## Known Issues

- None currently documented. Please see CHANGELOG.md for recent fixes.

## Future Enhancements

- Widget for quick stats
- Export/import filter rules
- Advanced user customization
- Multi-language support
