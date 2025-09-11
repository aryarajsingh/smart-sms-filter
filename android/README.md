# Smart SMS Filter - Android App

## Overview

The Android implementation of Smart SMS Filter uses the **Spam and Call Protection** service framework to filter SMS messages without needing to be the default messaging app.

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

### SMS Filtering Service
- Implements `CallScreeningService` for spam protection
- Real-time SMS classification using TFLite model
- No need to be default SMS app

### Three-Category System
1. **Inbox**: Important messages (OTPs, bank alerts, personal)
2. **Filtered**: Spam and promotional messages (silenced)
3. **Needs Review**: Uncertain classifications for user review

### On-Device AI
- Gemma 2B model (4-bit quantized)
- 100% local processing - no data sent to servers
- Continuous learning from user feedback

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

## Permissions Required

### Essential Permissions
```xml
<!-- SMS filtering -->
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />

<!-- Call screening service -->
<uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />

<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Architecture Details

### MVVM + Clean Architecture
- **UI Layer**: Jetpack Compose screens
- **Presentation Layer**: ViewModels managing UI state
- **Domain Layer**: Use cases and business logic
- **Data Layer**: Repository pattern with Room database

### Key Components
- `SmsFilterService`: Main filtering service
- `MessageClassifier`: TFLite model wrapper
- `MessageRepository`: Data access layer
- `FilterViewModel`: UI state management

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

- None currently documented

## Future Enhancements

- Widget for quick stats
- Export/import filter rules
- Advanced user customization
- Multi-language support
