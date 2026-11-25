# Clash Tracker Lite (experimental)

A rewritten, compact Android overlay for replay analysis. Only opponent elixir estimation and observed cycle. No automatic gameplay, OCR, account credentials, or PC required.

## Use
Install the debug APK, grant permission to draw over other apps and capture the entire screen, then open a standard 1v1 replay. Press Inicio at the first moment of the match. The overlay can be dragged via its title. Automatic image recognition and the approximate clock then update the elixir and the last four detected plays. Pause and start resets the estimate.

## Limitations
This version recognizes only 12 troop classes: archers, bomber, giant, goblin cage, goblin hut, goblins, knight, mini pekka, minions, musketeer, skeletons, spear goblins. Spells and other cards cannot be reliably counted; unknown classes may be misclassified. Badge calibration, confidence threshold and motion grouping are unvalidated on real phones. Cycle values 4 to 1 are approximate plays remaining for those observed cards in a normal cycle, not the opponent's full hand. Elixir starts at 5 with standard 1x/2x/3x schedule; misses, timing errors and special mechanics cause drift. Not a guaranteed accurate counter. Supercell may sanction live third-party assistance; prefer replays.

## Attribution
Badge detection and crop preprocessing adapted from bytelabs-oss/clash-jev, copyright 2026 bytelabs-oss, MIT license. The pretrained model and license are fetched at the pinned revision c1f4893c1627d593938d53910c2568d1addcb8d2 at build time and bundled into the APK. Source: https://github.com/bytelabs-oss/clash-jev. Clash Royale and its assets belong to Supercell; this is unofficial.
