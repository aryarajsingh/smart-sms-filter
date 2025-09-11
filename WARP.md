# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Smart SMS Filter is an intelligent Android SMS filtering application that uses on-device AI to automatically categorize and filter messages. The project uses TensorFlow Lite for ML inference and follows Clean Architecture with MVVM pattern.

**Key Technologies:**
- Android: Kotlin + Jetpack Compose
- AI/ML: TensorFlow Lite with Gemma 2B (fine-tuned and quantized)
- Architecture: MVVM + Clean Architecture + Room Database
- Build System: Gradle with Android Gradle Plugin 8.1.2
- Dependency Injection: Hilt

## Development Commands

### Android Development
```powershell
# Navigate to Android project
cd android

# Build the project (debug)
.\gradlew build

# Build release version
.\gradlew assembleRelease

# Run unit tests
.\gradlew test

# Run Android instrumented tests
.\gradlew connectedAndroidTest

# Install debug APK to connected device
.\gradlew installDebug

# Clean build artifacts
.\gradlew clean

# Lint check
.\gradlew lint

# Generate code coverage report
.\gradlew testDebugUnitTestCoverage
```

### Development Setup
```powershell
# Run the automated development setup script
.\scripts\setup-dev.ps1

# Manual setup alternative:
# 1. Open Android Studio
# 2. Import the 'android' directory as a project
# 3. Sync Gradle files
# 4. Add TFLite model to android/app/src/main/assets/
```

### Model Training (Python Environment)
```powershell
# Navigate to models directory
cd models

# Activate Python virtual environment
.\venv\Scripts\Activate.ps1

# Generate synthetic training data
python training/data_preprocessing.py --generate-synthetic --size 100000

# Train the model
python training/train_model.py --data datasets/combined_training_data.csv --epochs 10

# Convert to TensorFlow Lite
python training/convert_to_tflite.py --model checkpoints/best_model.h5 --quantize 4bit

# Copy model to Android assets
cp tflite/sms_classifier_model.tflite ../android/app/src/main/assets/
```

## Architecture Overview

### High-Level Architecture
The application follows Clean Architecture principles with clear separation of concerns:

1. **UI Layer** (`ui/` package)
   - Jetpack Compose screens and components
   - UI state management and user interactions

2. **Presentation Layer** (`presentation/` package)
   - ViewModels handling UI state and business logic coordination
   - Screen-specific state classes

3. **Domain Layer** (`domain/` package)
   - Use cases encapsulating business logic
   - Domain entities and interfaces
   - Repository contracts

4. **Data Layer** (`data/` package)
   - Repository implementations
   - Room database entities and DAOs
   - Local data sources

5. **ML Layer** (`ml/` package)
   - TensorFlow Lite model integration
   - Message classification logic
   - Model loading and inference

### Core Components Architecture

**SMS Filtering Service:**
- Implements `CallScreeningService` for spam protection
- No need to be the default SMS app
- Real-time classification using on-device AI

**Three-Category Classification System:**
1. **Inbox**: Important messages (OTPs, bank alerts, personal messages)
2. **Filtered**: Spam and promotional messages (silenced notifications)
3. **Needs Review**: Uncertain classifications for user review

**AI Model Integration:**
- Gemma 2B model (4-bit quantized to ~50MB)
- 100% local processing - no data sent to external servers
- Continuous learning from user feedback
- Input: Tokenized SMS text (max 512 tokens)
- Output: Classification probabilities [spam, important, uncertain]

### Database Schema
Uses Room database with the following key entities:
- `MessageEntity`: Stores SMS messages with classifications
- `UserFeedbackEntity`: Tracks user corrections for model improvement
- `FilterRuleEntity`: Custom user-defined filtering rules

### Project Structure
```
smart-sms-filter/
├── android/                    # Android app implementation
│   ├── app/src/main/java/com/smartsmsfilter/
│   │   ├── ui/                # Compose UI components
│   │   ├── presentation/       # ViewModels and UI state
│   │   ├── domain/            # Use cases and entities
│   │   ├── data/              # Repository and database
│   │   └── ml/                # TensorFlow Lite integration
│   └── build.gradle           # App-level dependencies
├── models/                    # AI model training and assets
│   ├── tflite/               # Production TensorFlow Lite models
│   ├── training/             # Python training scripts
│   └── datasets/             # Training data
└── scripts/                  # Build and setup scripts
```

## Development Guidelines

### Android Development
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- Use Kotlin coroutines for asynchronous operations
- Follow Material Design 3 guidelines with Jetpack Compose
- Implement proper permission handling for SMS access
- Use Hilt for dependency injection throughout the app

### AI Model Development
- Models must be optimized for mobile devices (<100MB)
- Inference time should be <100ms on average Android device
- All processing must happen on-device for privacy
- Use 4-bit quantization for model size optimization
- Test models on diverse Android devices before deployment

### Security & Privacy Requirements
- **100% On-Device Processing**: No SMS data sent to external servers
- **Minimal Permissions**: Only request essential SMS access permissions
- **Local AI Models**: All filtering happens locally on device
- Encrypted local database storage for sensitive data

### Testing Strategy
- **Unit Tests**: Focus on ViewModels, use cases, and ML model validation
- **Integration Tests**: Database operations and service functionality
- **UI Tests**: Compose UI testing and user interaction flows
- **Performance Tests**: Model inference timing and memory usage

### Code Organization
- Follow Clean Architecture boundaries strictly
- Use dependency injection for testability
- Separate concerns between UI, business logic, and data layers
- Keep ML model integration isolated in dedicated package
- Implement proper error handling and logging

### Model Training Workflow
1. Generate synthetic SMS data using LLM (Gemini 2.5 Pro)
2. Combine with public datasets and India-specific patterns
3. Train base model with focus on Indian SMS patterns
4. Fine-tune for local banking, OTP, and e-commerce formats
5. Apply 4-bit quantization for mobile optimization
6. Validate on diverse test sets before deployment

## Common Development Tasks

### Adding New UI Screens
1. Create Compose function in `ui/` package
2. Add corresponding ViewModel in `presentation/`
3. Define screen state classes
4. Add navigation route in main navigation graph
5. Write UI tests for new components

### Implementing New Classification Features
1. Update domain entities and use cases
2. Modify ML model input/output handling
3. Update database schema if needed
4. Add new UI components for feature display
5. Test end-to-end classification flow

### Model Updates
1. Train new model version in `models/training/`
2. Convert to TensorFlow Lite with appropriate quantization
3. Update model metadata and version info
4. Test inference performance on target devices
5. Deploy through app update mechanism

### Adding Custom Filter Rules
1. Define new rule entities in data layer
2. Create use cases for rule management
3. Implement UI for rule configuration
4. Integrate rules with main classification pipeline
5. Add export/import functionality for user convenience
