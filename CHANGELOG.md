# Change Log - Smart SMS Filter

All notable changes to this project will be documented in this file.

## [1.1.1] - 2025-09-12
### Added
- Settings screen (Theme mode, Filtering strength, Important types, Learning toggle)
- Explainability “Why?” bottom sheet; reads reasons from classification audit
- Loading skeletons for message list

### Changed
- OTP notifications are never silent; refined channel selection
- Top app bar includes Settings action on main tabs
- Navigation and banners improved for default SMS app role prompt

### Removed
- Legacy Onboarding (file retired; Premium Onboarding is the only flow)

### Documentation
- Updated README and ARCHITECTURE with Why? feature, notifications policy, default SMS role

## [1.1.0] - 2025-09-11
### Documentation
- Added comprehensive development workflow (DEV_WORKFLOW.md)
- Created automated version management script (version-bump.ps1)
- Established AI assistant context persistence system
- Enhanced README with current development context
### Added
- Premium Welcome Screen with value proposition, feature highlights, and spring animations
- Enhanced Onboarding Flow with progress header, better grouping, and polished copy
- Preferences integration into classification (Filtering Mode, Important Types, Spam Tolerance)
- Premium message thread UI: refined bubbles, composer, and animations

### Changed
- Theming system updated to dynamic iOS-esque palette with Material You compatibility
- Typography and spacing improvements across screens

### Fixed
- Removed inappropriate “iMessage” placeholder text
- Resolved onboarding having no effect on classification logic
- Ensured welcome content is visible above bottom action bar

## [1.0.0] - 2025-09-11
### Added
- Initial project structure, SMS reading, sending, contacts integration
- Basic rule-based classifier
- Compose screens for Inbox, Filtered, Needs Review, Thread, Compose Message
