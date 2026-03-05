# Preview System

## Files

- `ui/settings/ScreenPreview.kt` - Main preview composables for both app drawer and home screen
- `ui/settings/HomeScreenBitmapPreview.kt` - Bitmap-based home screen preview (PixelCopy screenshot)
- `ui/settings/SettingsScreen.kt` - Hosts the preview sections in tabbed layout (App Drawer / Home Screen tabs)

## App Drawer Preview (`AppDrawerPreviewSection` / `RealAppDrawerPreview`)

**Technology:** Compose-based reconstruction from live data
**Render order (back to front):**
1. Wallpaper background (actual device wallpaper via `WallpaperManager`)
2. Semi-transparent overlay (controlled by transparency slider)
3. Status bar (clock, signal, wifi, battery icons)
4. Search bar mockup
5. App grid (`LazyVerticalGrid`) with real app icons and folder previews
6. Custom scrollbar overlay (width/height/color configurable)
7. Navigation dots (3 circles at bottom)
8. Play button overlay (center, launches drawer preview)

**Data source:** `PackageManager` query for installed apps, drawer folders from SharedPreferences

## Home Screen Preview (`HomeScreenPreviewSection` / `HomeScreenPreview`)

**Technology:** Compose-based reconstruction from saved home screen data
**Render order (back to front):**
1. Wallpaper background (actual device wallpaper)
2. Status bar (clock, signal, wifi, battery)
3. App grid with placed apps, folders (2x2 mini-icon preview), and widgets
4. Empty cell markers (+ icons for unoccupied cells)
5. Page indicator dots
6. Dock bar at bottom with dock apps
7. Navigation bar
8. Play button overlay (center, launches home screen preview)

**Data source:** `HomeScreenApp` list, `HomeDockApp` list, `HomeFolder` list, `PlacedWidget` list from SharedPreferences

## Home Screen Bitmap Preview (`HomeScreenBitmapPreviewSection`)

**Technology:** Bitmap screenshot via PixelCopy API
**How it works:**
1. `LauncherWithDrawer` captures a screenshot of the actual home screen using `PixelCopy`
2. Saved to `{filesDir}/home_screen_preview.jpg`
3. Preview loads and displays the bitmap with `BitmapFactory` (downsampled 2x for memory)
4. Refreshes on lifecycle resume

**Currently not used in SettingsScreen** (replaced by the Compose-based `HomeScreenPreviewSection`)

## Navigation

Tapping either preview navigates to the respective live screen:
- App drawer preview -> launches `app_drawer` activity
- Home screen preview -> launches `launcher_preview` activity
- Both use `onPreviewDrawer` / `onPreviewLauncher` callbacks wired through `MainActivity`
