# Clash Tracker Pro — experimental

Android local screen analysis for standard match replays. Only inferred opponent elixir and observed card cycle, no automated gameplay. Separate application ID from Lite.

The build exports the 155-class YOLO26s detector published by PolarisMurray/Clash_AI (repository MIT license, pinned source revision 7de479bfc9ad70c8238741b6b6629cd6bf01716c). Labels come directly from checkpoint metadata, and ONNX output is checked before APK compilation. Card costs are drawn from the public RoyaleAPI/cr-api-data reference. The 187-class SudhishNeel model is not distributed here because its license terms are unspecified. See upstream model and tool licenses; this project does not assert a separate weight license beyond upstream's publication.

155 classes are visual categories, not 155 distinct playable cards. A detection only enters the tracker if its canonical name has a known reference cost and a red-color evidence heuristic succeeds. Unknown labels, spell effects, side ambiguity, duplicate spawns, variant costs, new cards, summoned units and special mechanics may be missed or misclassified. The cycle is an observed approximation and the elixir is inferred, not read from game memory. The actual accuracy and speed must be measured on a real device. Use recorded games or replays; third-party live assistance may violate Supercell rules.

Source: https://github.com/PolarisMurray/Clash_AI
Reference costs: https://github.com/RoyaleAPI/cr-api-data
