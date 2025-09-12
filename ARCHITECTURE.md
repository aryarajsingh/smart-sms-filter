# Smart SMS Filter - Architecture Documentation

## Overview
Smart SMS Filter is a production-ready Android application that uses AI and rule-based algorithms to intelligently classify SMS messages into categories: Inbox (important), Filtered (spam), and Needs Review (uncertain).

## Architecture Pattern
- **Clean Architecture** with clear separation of concerns
- **MVVM** presentation pattern with Jetpack Compose
- **Repository Pattern** for data abstraction
- **Dependency Injection** using Hilt
- **Reactive Programming** with Kotlin Flows

## Layer Structure

### 1. Presentation Layer (`presentation/`)
- **UI Components**: Jetpack Compose screens and reusable components
- **ViewModels**: Handle UI state and business logic coordination
- **Navigation**: Type-safe navigation between screens

### 2. Domain Layer (`domain/`)
- **Models**: Core business entities (SmsMessage, MessageCategory)
- **Repository Interfaces**: Contracts for data access
- **Use Cases**: Single-responsibility business operations
- **Classification Interfaces**: Contracts for AI and rule-based classification

### 3. Data Layer (`data/`)
- **Repository Implementations**: Concrete data access implementations
- **Database**: Room database with proper entities and DAOs
- **Classification Engines**: Rule-based and AI-powered classifiers
- **SMS Integration**: Broadcast receivers and system integration

## Key Components

### SMS Classification Pipeline
```
SMS Received -> SMS Receiver -> Classification Service -> Database -> UI Update
                     ↓              ↓
               Permission Check  Rule Engine + AI Model
```

### Classification System
- **Rule-Based Classifier**: Pattern matching, keywords, sender analysis
- **AI Classifier**: TensorFlow Lite neural network inference (planned)
- **Hybrid Approach**: Combines both with confidence scoring
- **Learning System**: Improves from user corrections
- **Explainability**: Classification Audit table stores reasons; UI exposes a “Why?” bottom sheet

### Database Schema
```sql
sms_messages:
- id (PRIMARY KEY)
- sender (TEXT)
- content (TEXT) 
- timestamp (INTEGER)
- category (TEXT)
- isRead (BOOLEAN)
- threadId (TEXT, NULLABLE)
- createdAt (INTEGER)

classification_audit:
- id (PRIMARY KEY)
- messageId (INTEGER, NULLABLE)
- classifier (TEXT)
- category (TEXT)
- confidence (REAL)
- reasonsJson (TEXT, pipe-delimited)
- timestamp (INTEGER)
```

## Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material3
- **Architecture Components**: ViewModel, Room, Navigation
- **Dependency Injection**: Hilt
- **Notifications**: Multiple channels; OTPs are never silent
- **AI/ML**: TensorFlow Lite (planned integration)
- **Async**: Kotlin Coroutines + Flow
- **Build System**: Gradle with Version Catalogs

## Performance Considerations
- **Background Processing**: Classification runs on background threads
- **Database Optimization**: Indexed queries and efficient pagination
- **Memory Management**: Proper lifecycle management and resource cleanup
- **Battery Optimization**: Efficient background processing

## Security & Privacy
- **On-Device Processing**: No data leaves the device
- **Minimal Permissions**: Only necessary SMS permissions requested
- **Data Encryption**: Sensitive data encrypted in local storage

## Testing Strategy
- **Unit Tests**: Domain logic and individual components
- **Integration Tests**: Database and classification pipeline
- **UI Tests**: User interactions and navigation
- **Performance Tests**: Classification speed and memory usage

## Deployment Pipeline
- **Debug Builds**: Development and testing
- **Release Builds**: Production-ready APKs with optimizations
- **Signing**: Proper code signing for Play Store
- **Analytics**: Crash reporting and usage analytics
