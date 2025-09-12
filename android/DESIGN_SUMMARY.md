# Design summary — Smart SMS Filter (Cohesiveness + Polish)

Goals
- Deliver a calm, premium feel with consistent typography, spacing, and micro‑interactions.
- Keep headers consistent (title + settings), limit layout shifts, and ensure meaningful “Why?” explanations.
- Maintain green builds while iterating; no device installs until design pass is approved.

Typography
- Bridged Material 3 typography to our iOS‑inspired ramp (IOSTypography) so components using MaterialTheme.typography now inherit the iOS styles.
  - Changes in: app/src/main/java/com/smartsmsfilter/ui/theme/Type.kt
  - Mappings: headline/title/body/label families point to IOSTypography aliases (e.g., headlineLarge -> Title1), ensuring consistent type across screens.

Spacing and Layout
- Adopted an 8–16dp spacing rhythm via IOSSpacing.
  - Screen header paddings normalized to 16dp horizontal, 16dp top, small bottom.
  - Conversation card internal spacing uses IOSSpacing.small (8dp) for vertical rhythm.
  - Empty states and lists use medium horizontal (16dp) and small vertical (8dp) paddings.

Headers and Settings
- Settings gear remains inline with the bold screen title; the global top bar is reserved only for the default SMS app banner (when needed), reducing layout shifts.

Conversation Cards
- Subtle interaction animations (scale/elevation) with clear visual selection state.
- Avatar and badge sizes standardized; Why? button uses an 18dp icon within a 28dp hit target.
- Category badge copy: “Important”, “Spam”, “Review”.

Transitions & Motion
- List entrance animation (light fade/slide) for first load/refresh (120–160ms).
- Dialogs use a subtle fade/scale entrance for a premium feel.

“Why?” Dialog and Corrections
- Always shows reasons via ExplainMessageUseCase combining:
  - Manual overrides and sender preferences (pinned/auto‑spam)
  - Hard rules (OTP)
  - Contextual reasons (private, on‑device)
- Maps internal signals to clear copy: “Pinned sender”, “Sender marked auto‑spam”, “OTP detected”, etc., with deduplication and ordering.
- Adds correction actions: Move to Inbox / Mark Spam with undo; logs user feedback to the audit table; quick reason chips for corrections: Not spam, Important person, OTP, Banking, Promotional.

Diagnostics
- Settings > Diagnostics gives quick access to set default SMS app (RoleManager) and open app settings; disables actions when already default.

Empty States
- Clean, empathetic copy and spacing per tab (Inbox/Spam/Review) using consistent typography and colors.

Quality & Build
- Unit tests added for ExplainMessageUseCase fallback pathways (OTP, pinned, auto‑spam, merge/dedupe).
- Build kept green (testDebugUnitTest).
- Note: animateItemPlacement was temporarily removed due to unresolved extension with current Compose BOM (2023.10.01). We can re‑introduce after a Compose bump or verifying the proper artifact/import.

Next Steps
- Classification logic deep‑dive (focused improvements with tests).
- Optionally re‑introduce list item placement animation post‑Compose update.
- Broader typography/spacing audit on secondary screens (if needed).

