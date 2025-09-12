# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by Keep a Changelog. Dates are in YYYY-MM-DD.

## [1.2.0] - 2025-09-12
### Added
- Welcome flow explains the product first, then asks for permissions (no prompts on first launch).
- Privacy pledge on Welcome: all processing happens on-device; no data leaves the phone.
- Soft checklist under "Get Started" hinting required actions (permissions, default SMS) without hard-gating.
- Shared, premium composer bar (rounded input, clean send button) used across Thread and Compose screens.
- Subtle, stable list micro-animations (fade/scale) for message items; avoids fragile dependencies.
- AutoMirrored icons for Back/Forward/Send/Help to improve RTL support and remove deprecations.
- Accessibility improvements: meaningful contentDescriptions for key icons.
- Diagnostics (Settings) refinements: default SMS CTA and app settings shortcut.
- "Why?" dialog actions with correction chips and feedback audit logging; ExplainMessageUseCase fallback ensures meaningful reasons.

### Changed
- Deferred permissions and default SMS prompts from app start to user-initiated actions from Welcome.
- Tightened top bar to only show SMS default banner post-onboarding to avoid layout shifts.
- Normalized paddings/typography across headers, dialogs, and lists for a calm, premium feel.

### Fixed
- Build stability: imports, icon deprecations, small warnings; tests passing.
- Minor copy and spacing polish across screens.

## [1.1.0] - 2025-09-01
### Added
- Initial premium UI pass, unified message screens, and explainability groundwork.
- Diagnostics section in Settings.

### Fixed
- Assorted build and layout fixes.

[1.2.0]: https://example.com/releases/1.2.0
[1.1.0]: https://example.com/releases/1.1.0
