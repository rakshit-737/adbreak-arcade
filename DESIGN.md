# AdBreak Arcade design notes

AdBreak Arcade uses a quiet, premium arcade-instrument direction rather than a
loud casino aesthetic. The main app is a light/dark Material-style surface with
a violet primary accent, generous spacing, readable system typography, and
short descriptive game cards. The notification is intentionally calmer: native
System UI owns the outer shell, while the app supplies compact violet-accented
controls and a separate expanded layout.

## Durable rules

- Violet is the action color; it should not be used for every label.
- Game state, score, and the next action must be visible before decorative copy.
- App controls use native Android touch targets and honor system insets.
- Notification content stays compact, quiet, and understandable without the app
  being open.
- Dark mode is a first-class palette, not an inverted screenshot.

## Smart App Trigger settings

- Treat Usage access as a sensitive, opt-in capability.
- Show clear disabled, permission-needed, active, and unavailable states.
- Explain that selected package names and trigger history are stored locally.
- Provide explicit controls for disabling triggers and clearing selected apps.
- Never imply ad detection or inspection of another app's content.
- State that trigger timing is best effort rather than guaranteed immediate behavior.

## Expanded library

- Library count and cards come from `GameRegistry`, never hard-coded copy.
- Every game exposes a concise title, description, and notification-sized actions.
- Test long labels, emoji, Unicode symbols, large font scale, dark mode, and
  collapsed/expanded notification layouts on actual System UI.
