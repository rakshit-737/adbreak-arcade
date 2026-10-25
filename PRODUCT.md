# AdBreak Arcade
<!-- impeccable:product-schema 1 -->

## Platform
android

## Stack
User-selected: Kotlin, native Android Views and XML, Gradle Kotlin DSL,
NotificationCompat and RemoteViews. Local persistence, no backend.

## Users and purpose
People waiting through a video advertisement can manually expand a notification
and play a short game without deliberately opening another Activity per move.

## Constraints
Seven offline games. No ad detection, blocking, skipping, overlays, accessibility
service, notification listener, analytics, network permission, or foreground service.
Android owns notification layout and may close the shade or pause foreground media.
The app must not promise device behavior it has not tested.

## Brand commitments
Premium, restrained violet accent; readable native controls; light and dark themes.
Main app screens: Home, Library, game details, setup, scores, settings.

## Accessibility
Scalable text, descriptive button labels, native focus behavior, 48dp app controls.
Notification controls must remain compact and be checked on actual System UI.

## Implementation decisions delegated by user
Use a quiet, instrument-panel-like game launcher with a live state preview, clear
notification status, and one prominent Start action. Follow the system theme by
default. These are implementation assumptions, not additional product claims.
