# Smart SMS Filter

An Android SMS management app with intelligent on-device message classification.

## Features

- **Automatic Classification**: Messages are sorted into Inbox, Spam, or Needs Review
- **Privacy-First**: All processing happens on-device, no data leaves your phone
- **Learning System**: Adapts to your preferences without storing personal data
- **Fast**: Classifications in 65ms, contact lookups in <1ms

## How It Works

### Classification Engine

Our hybrid classification system combines multiple approaches:

1. **Rule-Based Engine**
   - Pattern matching for OTPs, banking messages, and known spam patterns
   - Keyword analysis with weighted scoring
   - Sender reputation tracking based on user corrections
   - Contact whitelist - messages from contacts always go to inbox

2. **Machine Learning** (Optional)
   - TensorFlow Lite model for advanced classification
   - 4-bit quantized for efficiency
   - Runs entirely on-device
   - Located in `app/src/main/assets/mobile_sms_classifier.tflite`

3. **Contextual Analysis**
   - Time-based patterns (business hours vs off-hours)
   - Message frequency analysis
   - Conversation threading
   - Sender history evaluation

The system uses a confidence-based approach:
- High confidence (>0.8): Direct classification
- Medium confidence (0.5-0.8): Additional context considered
- Low confidence (<0.5): Marked for review

### Architecture

```
app/
├── classifier/          # Unified classification engine
│   ├── UnifiedSmartClassifier.kt    # Main orchestrator
│   ├── RuleEngine.kt                # Pattern matching
│   └── LearningEngine.kt            # User preference learning
├── data/               # Data layer
│   ├── database/       # Room database
│   ├── contacts/       # Contact management with caching
│   └── sms/           # SMS operations
├── domain/            # Business logic
│   ├── model/         # Data models
│   └── usecase/       # Use cases
└── presentation/      # UI layer (Jetpack Compose)
```

## Installation

### Pre-built APK
```bash
# Download from releases
wget https://github.com/aryarajsingh/smart-sms-filter/releases/latest/download/app-release.apk
adb install app-release.apk
```

### Build from Source
```bash
git clone https://github.com/aryarajsingh/smart-sms-filter.git
cd smart-sms-filter/android
./gradlew assembleRelease
# APK will be in app/build/outputs/apk/release/
```

## Requirements

- Android 7.0+ (API 24)
- Permissions: SMS, Contacts, Notifications
- Storage: ~100MB (includes ML model)

## Development

### Setup
1. Clone the repository
2. Open `android/` folder in Android Studio
3. Sync Gradle files
4. Run on device/emulator

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Database**: Room
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **ML**: TensorFlow Lite (optional)

### Key Classes

- `UnifiedSmartClassifier`: Main classification orchestrator
- `SmsReceiver`: Intercepts incoming messages
- `SmartNotificationManager`: Handles intelligent notifications
- `ContactManager`: Manages contact lookups with LRU caching
- `SmsViewModel`: Primary UI state management

## Performance

| Metric | Value | Details |
|--------|-------|---------|
| Classification Speed | 65ms | 3x faster than v1.0 |
| Contact Lookup | <1ms | LRU cache with 100 entry limit |
| Memory Usage | 85MB | Including ML model |
| Database Queries | <10ms | Indexed on sender, timestamp, category |
| UI Frame Rate | 60 FPS | Optimized LazyColumn with keys |

## Privacy & Security

- **No Network Access**: Core features work offline
- **No Analytics**: Zero tracking or telemetry
- **Encrypted Storage**: Sensitive data encrypted with Android Keystore
- **Open Source**: Full code transparency

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate coverage report
./gradlew testDebugUnitTestCoverage
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Documentation

- [Architecture Details](UNIFIED_ARCHITECTURE.md)
- [Technical Fixes](TECHNICAL_FIXES.md)
- [Changelog](CHANGELOG.md)

## License

MIT License - see [LICENSE](LICENSE) file

## Contact

- Issues: [GitHub Issues](https://github.com/aryarajsingh/smart-sms-filter/issues)
- Email: aryarajsingh@example.com