# Clash AI New

Android app focused on showing the opponent's 8-card deck during a live Clash Royale match.

## Project status
The app now includes an Android MediaProjection foreground service that captures screen frames after explicit user consent. Android 13 notification permission is requested before capture. The current build does not yet classify cards or detect match state; the eight slots remain placeholders.

## Planned implementation
1. ~~Android MediaProjection capture with explicit user consent.~~ Implemented as a foreground service.
2. Match-state detection from captured frames.
3. Visual recognition of cards played by the opponent.
4. Deduplication and reconstruction of the opponent's 8-card deck.
5. On-device inference and an optional overlay, without reading game memory, injecting into Clash Royale, or automating taps.

## API note
RoyaleAPI's former public developer API was sunset. Supercell's official Clash Royale API offers a recent battle-log endpoint for a player's own tag, but it does not identify an unknown opponent's full deck live during an ongoing match. Live deck reconstruction therefore depends on visual recognition.

## Build
GitHub Actions builds a debug APK on every push to main and on manual workflow dispatch.
