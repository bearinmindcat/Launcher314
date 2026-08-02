# Release Notes for v0.0.22-beta

## What's New

You can now put folders inside of folders in the app drawer (sub-folders) — drag a folder onto another folder or use its long-press menu; back button steps out one level at a time - issue #71

Folders can now be added to tabs, and apps that live inside folders now show up correctly on tabs they're assigned to (they used to just disappear) - issue #69

Folders can now be included in drawer sorting with the new "Add folders to sorting" toggle — Name, Size and date sorting all apply to folders too, and they sort in among the apps instead of being stuck at the top - issue #72

You can now pick an app's icon straight from any installed icon pack — the "Icon" button asks Picture or Icon pack, and the pack browser shows every icon in the pack with a "Suggested for this app" shortcut at the top - issue #70

Added a "Hide search bar" toggle to Additional Drawer Settings - issue #67
Added a "Hide scrollbar" toggle (auto-hides the scrollbar when not in use)
Added a "Tabs at bottom" toggle — combine with "Swipe between tabs" for a Nova-style bottom tab bar

Widgets now size themselves from your ACTUAL grid instead of a hardcoded formula, so big widgets no longer claim to need more cells than your grid has ("not enough space" on an empty screen is fixed)
Widgets no longer break when you change grid sizes — they re-flow, shrink to fit and spill to the next page instead of stacking on top of each other

Reworked the drawer close gesture to work like Lawnchair / Neo: a fast scroll to the top stops with a bounce (second swipe closes), spam-swiping down closes reliably every time, the whole drawer panel does a mini bounce when a fling hits the top, and the open bounce is gentler

Tabs can now be reordered by dragging them right in the drawer, and long-pressing a freshly made tab opens its edit menu correctly
Fixed renaming a folder through the customize popup not updating the label
Folder icons now render identically everywhere (drawer, drag, previews, sub-folders, tab editor)
Removed the version section from Development Information (it's already in App Info)
