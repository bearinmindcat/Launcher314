# Font System

## Files

- `helpers/FontManager.kt` - Core font management: 172 bundled fonts, custom font import/delete, selected font lookup
- `ui/settings/FontsScreen.kt` - Font selection UI with search, preview (each font shown in its own style), import/delete
- `ui/theme/Theme.kt` - App theme (fonts NOT applied globally; only app labels use selected font)
- `data/ScreenSaveState.kt` - Persistence: `selected_font_id` and `imported_font_paths` in SharedPreferences

## Where Selected Font Is Applied

- Home screen app labels (`ui/home/LauncherScreen.kt`)
- Home screen folder labels (`ui/home/LauncherScreen.kt`)
- Dock app labels (`ui/home/LauncherScreen.kt`)
- App drawer app labels (`ui/drawer/AppDrawerScreen.kt`)
- App drawer folder labels (`ui/drawer/AppDrawerScreen.kt`)
- Preview screens (`ui/settings/ScreenPreview.kt`)

## Where System Default (Roboto) Is Used

- All settings UI (buttons, sliders, toggles, section headers)
- Fonts screen UI (search bar, buttons, dialogs)
- All Material3 components (dialogs, text fields, top bars)

## Font Resources

All 172 bundled font files live in `app/src/main/res/font/` as `.ttf` files.
Custom imported fonts are stored in `{app filesDir}/fonts/` at runtime.

## How It Works

1. User selects a font in `FontsScreen` -> `setSelectedFont(context, fontId)` saves to SharedPreferences
2. `FontManager.getSelectedFontFamily(context)` returns the `FontFamily` or null (= default)
3. Each screen reads the font on composition/resume: `FontManager.getSelectedFontFamily(context)`
4. App labels use: `fontFamily = selectedFontFamily ?: FontFamily.Default`
