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

## Website visual system

The project website uses the same restrained violet identity as the Android app,
expanded into a quiet intermission-console world: midnight instrument panels,
fine calibration lines, compact mono state labels, and authored compact/expanded
notification strata. Violet marks action and selection; it is not sprayed across
every label. Light mode uses paper, ink, and lilac surfaces while dark mode uses
midnight, raised card, and lavender text roles.

The website's primary experience mode is **Persuade**. Its first viewport must
make the notification mechanism visible, offer a working website demo, and keep
the two honest actions—explore the games and inspect the source—above the fold.
Conceptual notification artwork is labelled as such and never presented as an
OEM screenshot. The published site uses `/adbreak-arcade/` as its Vite base path.

Durable website rules:

- Use semantic landmarks, visible focus, keyboard-operable controls, and 48px
  target sizing for primary interactions.
- Respect `prefers-reduced-motion`; reveal motion is enhancement, not required
  content.
- Keep Android limits visible: no ad detection claim, no forced shade opening,
  no guaranteed trigger timing, and no promise of uninterrupted media playback.
- Keep the verified debug APK clearly labelled as a testing artifact until
  protected production signing exists.
