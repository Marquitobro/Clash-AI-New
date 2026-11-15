# Clash AI New

Android app focused on showing the opponent's 8-card deck during a live Clash Royale match.

## Project status
This repository starts with a clean Android/Jetpack Compose foundation and an APK build workflow. The current screen is a UI scaffold only. Automatic match detection and card recognition are not implemented yet.

## Planned implementation
1. Android MediaProjection capture with explicit user consent.
2. Match-state detection from captured frames.
3. Visual recognition of cards played by the opponent.
4. Deduplication and reconstruction of the opponent's 8-card deck.
5. On-device inference and overlay, without reading game memory, injecting into Clash Royale, or automating taps.

## API note
RoyaleAPI's former public developer API was sunset. Supercell's official Clash Royale API offers a recent battle-log endpoint for a player's own tag, but it does not identify an unknown opponent's full deck live during an ongoing match. Live deck reconstruction therefore depends on visual recognition.

## Build
GitHub Actions builds a debug APK on every push to main and on manual workflow dispatch.
