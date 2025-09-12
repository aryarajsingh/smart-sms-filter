## Version 1.1.0 - The Phoenix Update

This is a massive overhaul focused on performance, UI correctness, and code quality. The app should feel significantly faster, smoother, and more polished.

### Major Fixes & Improvements

*   **🚀 Radical Scrolling Performance Boost**: Fixed the choppy scrolling in message lists. All data processing (grouping, sorting) has been moved off the UI thread and into the ViewModel, ensuring a silky-smooth scrolling experience at the device's native refresh rate.
*   **🎨 Theming Overhaul (Light & Dark Mode)**: Systematically fixed all UI glitches, especially in light mode. Removed all hardcoded `alpha` values, custom gradients, and shadows that caused a "muddy" look. The UI now uses proper, opaque Material 3 color roles for a clean, modern, and high-contrast appearance.
*   **🐞 Spam Counter Fixed**: The spam message counter now updates instantly and accurately when messages are moved to or from the spam folder.
*   **🛡️ Stronger Spam Classification**: The spam filter is now more aggressive and accurate.
    *   **Airtel SPAM Rule**: A new, high-priority rule immediately blocks any message containing "Airtel Warning: SPAM" or similar patterns.

### Minor Fixes

*   **Thread Unification**: Fixed a bug where conversations with the same person could appear as multiple threads due to inconsistent phone number formatting.
*   **Full History**: The app now loads the complete message history for every conversation, not just the most recent messages.
*   **Duplicate Prevention**: Replaced the aggressive 60-second duplicate detection with a precise check, ensuring no legitimate messages are accidentally dropped.

### Code Quality & Housekeeping

*   **Full Codebase Audit**: Every single line of code was read and analyzed.
*   **Comprehensive Documentation**: Added clear KDoc comments to all major classes and functions across the data, domain, and presentation layers.
*   **Security**: Audited all user input points and data handling pathways.

# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by Keep a Changelog. Dates are in YYYY-MM-DD.

## [1.2.0] - 2024-12-09
### Security Enhancements
- **Encryption Manager**: Added Android Keystore-based encryption for sensitive data using AES/GCM with 256-bit keys
- **Rate Limiting**: Implemented comprehensive rate limiting for SMS sending (30/hour, 100/day, 5/hour per number) to prevent abuse
- **Input Validation**: Enhanced validation utilities with SQL injection and XSS prevention
- **Database Security**: Added proper database migrations to preserve user data during updates
- **Permission Checks**: Added runtime permission checks before all sensitive operations

### Bug Fixes
- **Critical Import Error**: Fixed phone number normalization utility missing in ContactManager and SmsReader
- **Dependency Injection**: Fixed SmsSenderManager DI with proper RateLimiter injection
- **Classification Bug**: Fixed incorrect parameter type in ClassificationServiceImpl audit logging
- **UI Compilation**: Fixed missing Modifier and LaunchedEffect imports in screen composables
- **Database Migration**: Added proper migration for isOutgoing column to prevent data loss

### Performance Improvements
- **Contact Loading**: Added phone number normalization caching to improve contact resolution speed
- **Database Queries**: Added proper indexes on frequently queried columns
- **Memory Management**: Fixed potential memory leaks in coroutine scopes

### Code Quality
- **Comprehensive Audit**: Reviewed every file in data, domain, and presentation layers
- **Documentation**: Added KDoc comments to all public classes and methods
- **Error Handling**: Improved error handling with proper Result types and AppException hierarchy
- **Code Organization**: Separated security utilities into dedicated package

### Infrastructure
- **Build System**: Updated Gradle dependencies and fixed all compilation warnings
- **Version Bump**: Updated to version 1.2.0 with comprehensive changelog

## [1.1.0] - 2025-09-01
### Added
- Initial premium UI pass, unified message screens, and explainability groundwork.
- Diagnostics section in Settings.

### Fixed
- Assorted build and layout fixes.

[1.2.0]: https://example.com/releases/1.2.0
[1.1.0]: https://example.com/releases/1.1.0
