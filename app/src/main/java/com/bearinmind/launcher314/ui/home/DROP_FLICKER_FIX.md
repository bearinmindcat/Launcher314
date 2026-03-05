# Drop-to-Original-Position Flicker Fix

## The Problem

When dragging an app icon and dropping it back to its original position, a visible flicker/flash occurred at the moment of drop. This affected the icon, the text label, grid cells, dock slots, and folder-inside cells.

## Root Causes & Fixes

Four issues combined to produce the flicker:

### 1. Cell Alpha Animation Lag (AppGridMovement.kt)

The cell content used `animateFloatAsState` with `snap()` to transition from hidden to visible:

```kotlin
val contentAlpha by animateFloatAsState(
    targetValue = if (isDragging) 0.8f else 1f,
    animationSpec = if (isDragging) tween(150) else snap(),
)
```

**Why it flickered:** `animateFloatAsState` internally uses `LaunchedEffect(targetValue)` to drive the animation. `LaunchedEffect` runs in the **effect phase**, which happens AFTER composition. So when `isDragging` flipped from true to false:

- **Frame N (composition):** `isDragging` = false, overlay disappears, cell alpha becomes `contentAlpha` — but `contentAlpha` is still 0.8f because `LaunchedEffect` hasn't fired yet
- **Frame N (effect phase):** `LaunchedEffect` fires, `snap()` sets `contentAlpha` to 1f
- **Frame N+1:** Cell renders at full alpha

Result: one frame where the cell appears at 0.8f alpha with no overlay = visible dim flash.

**Fix:** Removed the `contentAlpha` animation entirely. The animation served no visible purpose — the cell was always at alpha 0f during drag (the 0.8f target was invisible behind the 0f override). Changed to direct `0f`/`1f`:

```kotlin
alpha = if (isDragging && (cell is HomeGridCell.App || cell is HomeGridCell.Folder)) 0f else 1f
```

Applied to: grid cells, dock app slots, dock folder slots.

### 2. Label Alpha Animation Lag (AppGridMovement.kt)

Same `animateFloatAsState` + `snap()` bug, but on the text label:

```kotlin
val hideLabel = showContextMenu || isDragging
val labelAlpha by animateFloatAsState(
    targetValue = if (hideLabel) 0f else 1f,
    animationSpec = if (hideLabel) tween(durationMillis: 150) else snap(),
)
```

The icon appeared instantly (fixed by #1 above), but the text label still had its own animated alpha with the same one-frame snap delay.

**Fix:** Override the animated value with a direct `1f` when not hiding, while preserving the fade-out animation for context menus:

```kotlin
.graphicsLayer { alpha = if (!hideLabel) 1f else labelAlpha }
```

This uses `1f` directly when transitioning to visible (no snap delay), and keeps the animated `labelAlpha` fade-out when hiding (for the context menu long-press effect). Applied to both app labels (`labelAlpha`) and folder labels (`folderLabelAlpha`).

### 3. Integer Pixel Truncation (LauncherScreen.kt)

The overlay position used `.offset { IntOffset(appLeft.toInt(), appTop.toInt()) }` which truncated float coordinates to integers. The cell used Compose's layout system with sub-pixel precision. This mismatch caused a 0-1px jump when the overlay was replaced by the cell.

**Fix:** Merged the offset into `graphicsLayer` using float-precision translation:

```kotlin
.graphicsLayer {
    translationX = appLeft   // float precision — no truncation
    translationY = appTop
    scaleX = boxScale
    scaleY = boxScale
    alpha = boxAlpha
    clip = false
}
```

Applied to all 4 overlay sections: folder-escape, grid app, dock, and folder-inside.

### 4. Overlay Scale/Alpha/Text Mismatch on Return-to-Origin (LauncherScreen.kt)

During the drop animation, the overlay used animated values for scale (`1.265→1.0`), alpha (`0.8→1.0`), and text (`0→1`). For return-to-origin drops these transitions caused a visible mismatch — the overlay didn't match the cell at the moment of swap.

**Fix:** Added `dropTargetOffset == Offset.Zero` checks so return-to-origin drops use constant values matching the cell:

```kotlin
val boxScale = if (isDropAnimating && dropTargetOffset == Offset.Zero) 1f else ...
val boxAlpha = if (isDropAnimating && dropTargetOffset == Offset.Zero) 1f else ...
val textAlpha = if (isDropAnimating && dropTargetOffset == Offset.Zero) 1f else ...
```

Applied to all 4 overlay sections.

## Key Takeaway

`animateFloatAsState` with `snap()` is NOT truly instant — it has a one-frame delay because `LaunchedEffect` runs after composition. For transitions where an overlay and the underlying cell must swap in the exact same frame, use direct state values instead of animated values.

## Files Modified

- `AppGridMovement.kt` — Removed `contentAlpha` animation (fixes #1), overrode `labelAlpha`/`folderLabelAlpha` with direct 1f when visible (fix #2)
- `LauncherScreen.kt` — Float-precision overlay positioning via `translationX`/`translationY` (fix #3), constant scale/alpha/text for return-to-origin drops (fix #4)
