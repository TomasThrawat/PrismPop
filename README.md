# PrismPop

A match-3 puzzle game for Android, built in Kotlin from scratch. Swap adjacent gems (tap-then-tap or swipe) to line up 3+ of the same color; matches cascade, drop, and refill automatically, and score climbs as you clear.

## How it was built

- **Game logic** (`GameBoard.kt`): an 8x8 grid model handling swap validation, horizontal/vertical match detection, cascading clears with gravity, refill, scoring, and a "no moves left" reshuffle check. Pure Kotlin, no Android dependencies.
- **Rendering & input** (`GameView.kt`): a single custom `View` that draws the board and gem pieces on `Canvas` and handles tap/swipe gestures. No game engine or third-party framework — just Android's own drawing APIs.
- **Art assets** (`res/drawable/ic_gem_*.xml`, `ic_star.xml`, `ic_heart.xml`): generated procedurally through a custom **Engine MCP** asset server (`CUSTOM_ENGINE_MCP_GENERATE_ICON`, offline/no external calls) as flat vector SVGs, then converted to native Android `VectorDrawable` XML and wired in as the actual in-game gem sprites. The star and heart icons are included as reserved assets for a future power-up/bonus-gem feature and aren't used in the base gameplay yet.

## Project structure

```
app/src/main/java/com/tomasthrawat/prismpop/
  GemType.kt      - the 6 gem types + their generated drawable resources
  GameBoard.kt     - board state, matching, cascading, scoring
  GameView.kt      - drawing + touch input
  MainActivity.kt  - entry point
app/src/main/res/drawable/ - Engine MCP-generated gem/star/heart vector icons
.github/workflows/android-build.yml - CI: builds a debug APK on every push
```

## Build

Open in Android Studio and run, or build from the command line:

```
gradle assembleDebug
```

Every push to `main` also triggers a GitHub Actions build that uploads the debug APK as a workflow artifact.

## Rules

- 8x8 board, 6 gem colors, swap only between orthogonally adjacent cells.
- A swap is only kept if it creates a match of 3 or more in a row or column; otherwise it's reverted.
- Matches cascade: cleared cells drop the gems above them and refill from the top, repeating until the board is stable.
- +10 points per gem cleared (including cascades).
- If no valid move exists after a turn, the board automatically reshuffles.
