# Clash AI — Android experimental
Standalone Kotlin app for Android 8+. No PC required to run. This branch is a test version, not a complete autonomous player.

## Implemented
- MediaProjection capture after explicit consent and an Android system overlay.
- Real on-device ML Kit OCR, limited to visible text matching a partial Spanish/English card dictionary.
- Manual confirmation of opponent card observations; elapsed-time elixir estimate, manually selected regeneration multiplier, observed unique cards and last four plays.
- Simple textual suggestions based on estimated elixir, not a trained game-playing policy.
- Unit tests and APK CI.

## How to try
Install the debug APK from a successful Actions run. Open the app, allow overlay and full-screen capture, then open a replay or screen recording. Tap AI · Analizar, tap Nueva partida at the beginning of a standard match, and confirm only genuine opponent deployments. Choose x2 or x3 when the mode changes regeneration. Tap Cerrar to stop capture.
OCR is not a visual troop detector; names may not be shown in live play, and confirmation can be wrong or incomplete. The estimate cannot reveal the true hidden elixir and does not model every mechanic. The cycle is observed history, not a complete prediction. No autonomous taps or trained policy are included. Device testing and a trained visual detector are required before further functionality can be claimed.

Supercell prohibits gameplay bots and automation. Use for analysis of recordings/replays; no guarantee of account safety in live play.