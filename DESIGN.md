# AdBreak Arcade design system

AdBreak Arcade is a notification-first handheld arcade, not an ad blocker and
not a conventional mobile game platform. The product identity combines warm
editorial paper, industrial instrument labels, restrained arcade signals, and
native Android surfaces.

## Visual direction

- **Light:** paper `#F5F4EF`, ink `#171717`, raised paper `#EAE9E3`, line
  `#D6D5CE`.
- **Dark:** charcoal `#151515`, surface `#202020`, raised surface `#292929`,
  line `#45453F`.
- **Action:** muted orange `#E56B45` in light mode and `#F0805B` in dark mode.
  Orange marks actions, selection, and product status; it is not a page fill.
- **Secondary signal:** muted olive is used for readiness and family hints.
- **Type:** Manrope for interface copy, Newsreader for editorial emphasis, and
  DM Mono for technical labels, scores, and metadata.
- Avoid violet-led palettes, gradients, decorative blobs, glassmorphism,
  gratuitous rounded cards, stock imagery, and generic SaaS language.

## Website rules

The website uses an asymmetric editorial grid, compact navigation, warm neutral
surfaces, black/charcoal typography, thin rules, orange action marks, and real
product interfaces as the visual story. The hero and demo must make the
notification mechanism visible immediately.

The notification demonstration is explicitly labelled a **website simulation**
and implements actual tap, math, and higher/lower reducer logic. It supports
compact and expanded views, game switching, reset, keyboard focus, responsive
layout, and reduced-motion preferences. It never claims to be an OEM screenshot
or a live Android notification.

The catalogue is sourced from the shared website registry (`content.ts`) and
shows all 25 game entries with search, category filters, mechanics, and empty
state recovery. Product claims must remain factual: local state, no account, no
declared network permission, optional Smart App Triggers, no content inspection,
no ad detection, no forced shade opening, and no guaranteed trigger timing.

The production base path is `/adbreak-arcade/`. Semantic landmarks, visible
focus, keyboard-operable controls, a mobile menu, theme persistence, system
preference as the initial theme, metadata, Open Graph copy, and 48px primary
targets are required. Animation is a hint, never the only way to understand the
page.

## Immersive website experience 2.0

The website's second experience layer is a lightweight interactive arcade, not
a replacement app. It adds:

- A staged hero arrival for the conceptual Android notification shade, with an
  expandable notification and a small score loop.
- A desktop-only custom cursor and restrained magnetic CTAs. Both disappear on
  touch devices and under `prefers-reduced-motion`.
- IntersectionObserver reveals, chapter-based product storytelling, tactile
  cabinet hover states, and no scroll hijacking or continuous canvas loop.
- Eight browser cabinets backed by tested reducer logic: Tap Counter, Quick
  Math, Tic-Tac-Toe, Higher / Lower, Memory Challenge, Rock Paper Scissors,
  Lights Out Mini, and Number Sequence.
- A notification lab with compact/expanded states, game switching, pause/resume,
  reset, and feedback. It remains labelled as a browser simulation.
- Catalogue preview dialogs, local demo favourites, search, category filters,
  keyboard escape handling, and responsive touch-sized controls.

The experience intentionally uses CSS, SVG, DOM state, and small reducer
modules instead of WebGL or a large animation dependency. Heavy visual effects
would compete with the notification concept and make the GitHub Pages build
less portable.

## Android rules

The native app keeps Kotlin, Android Views/XML, `GameRegistry`, the existing
game interfaces, `SharedPreferences` state format, `NotificationCompat`,
`RemoteViews`, immutable `PendingIntent` envelopes, and Smart App Trigger
permission boundaries.

The launcher uses the same paper/charcoal/orange/olive family as the website:
brand header, local level progress, continue-playing selection, notification
availability, and recent play are prioritized before secondary settings.
The library uses family accents and clear text labels; color is never the only
identifier. Scores show records, session counts, and local progression without
locking or gating games. Settings explain notification permission and Usage
access before offering actions and provide system/light/dark theme override.

The notification remains the core surface. It must stay within Android
`RemoteViews` limits, keep objective/score/action hierarchy visible, support
compact and expanded layouts, expose accessible labels, preserve session and
render-revision safety, and remain legible in dark mode and large text. Android
System UI controls the outer shell and OEM-specific presentation.

## Smart App Triggers

Triggers are disabled by default and require explicit app selection plus Usage
access. They observe selected package foreground transitions only, store the
selection locally, and operate on a best-effort lifecycle-polling basis. They do
not inspect app content, identify advertisements, skip or manipulate ads, force
open the notification shade, or promise instant behavior.
