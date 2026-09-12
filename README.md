# PrismPop

A match-3 puzzle game for Android, built in Kotlin from scratch — start menu, a 10-level lobby, animated swaps/cascades, synthesized sound, and gem art generated procedurally through a custom Engine MCP asset server.

## Screens

- **Start menu** (`MainMenuActivity`) — title screen with a Play button and a sound on/off toggle (persisted).
- **Level lobby** (`LevelSelectActivity`) — grid of 10 levels; a level unlocks once the previous one is cleared (persisted in `SharedPreferences`).
- **Game screen** (`GameActivity` + `GameView`) — HUD (level / score-vs-target / moves left) above the board; win/lose dialog with Next Level / Retry / Back to Levels.

## How it plays

- 8x8 board, 6 gem types, swap only between orthogonally adjacent cells (tap-then-tap or swipe).
- A swap that creates no match animates back to its original spot and does **not** cost a move; a kept swap always costs exactly one move.
- Matches cascade: cleared gems shrink out, everything above drops down with a fall animation, new gems fall in from the top, and it repeats until the board is stable — all of it animated, nothing pops instantly.
- Each level has a target score and a move limit (both increase with level number). Reach the target before moves run out to clear the level and unlock the next one.

## Art — Engine MCP

Each gem type now has its own silhouette (not the same shape recolored), generated via the custom Engine MCP server's `CUSTOM_ENGINE_MCP_GENERATE_ICON` tool and converted from SVG path data into native Android VectorDrawables with a radial/linear gradient fill and a glossy highlight for depth:

| Gem | Shape (Engine MCP `kind`) |
|---|---|
| Red | heart |
| Orange | coin |
| Yellow | star |
| Green | gem |
| Blue | shield |
| Purple | potion |
| App icon | chest (custom gradient) |

## Sound

`SoundManager` synthesizes every effect (swap, invalid swap, match/combo, level win fanfare, level lose) as PCM16 sine tones at playback time via `AudioTrack` — there are no bundled audio files.

## Project structure

```
app/src/main/java/com/tomasthrawat/prismpop/
  MainMenuActivity.kt   - start screen
  LevelSelectActivity.kt- level lobby
  GameActivity.kt       - HUD + level win/lose flow
  GameView.kt           - board rendering, input, animation
  GameBoard.kt          - board state, matching, cascading, scoring (pure Kotlin)
  GemType.kt            - the 6 gem types + their generated drawable resources
  Level.kt              - level definitions (target score / move limit)
  Progress.kt           - SharedPreferences-backed unlock + sound-toggle state
  SoundManager.kt       - synthesized sound effects
app/src/main/res/drawable/ - Engine MCP-generated gem + launcher icon vectors
.github/workflows/android-build.yml - CI: builds a debug APK on every push
```

## Build

Open in Android Studio and run, or from the command line:

```
gradle assembleDebug
```

Every push to `main` also triggers a GitHub Actions build that uploads the debug APK as a workflow artifact.
