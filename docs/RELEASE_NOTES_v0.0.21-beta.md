# Release Notes for v0.0.21-beta

## What's New

### App Drawer Tabs
Reorder your drawer tabs right from the drawer — long-press a tab and drag it, just like moving an app icon (with a pickup vibration, a lift/enlarge, and a smooth spring back into place). A hold-in-place still opens the tab editor.  
The tab bar now auto-scrolls to keep the active tab's label on screen when you switch tabs by tapping or swiping (issue #62), in both Left and Right alignment.  
Tab alignment is now Left / Right only (Center was removed).

### Drawer Search
Added a "Fuzzy app search" toggle — find apps by initials (e.g. "yt" → YouTube) (issue #65).  
Added a "Recency search bias" toggle — while searching, apps you've opened recently rank higher (issue #64).  
Added a "Suggested apps bar" — a most-used-apps bar that appears at the top of the app list when you open search.  
Fixed a bug where tapping empty space (or a moving item) during a drawer search could open a random app.

### Settings Reorganization
Moved the drawer search/behavior options onto their own "Additional Drawer Settings" screen (Fuzzy search, Recency bias, Suggested apps, Launch from search, Reverse search bar, Auto open keyboard) to declutter the main Settings.  
Added an "Additional Home Screen Settings" screen for the home-screen gestures (double-tap to lock, swipe down, swipe right, double tap).  
Both new screens have a live, interactable preview pinned at the top — tap the play button to jump straight into the drawer / launcher — and a scrollbar that follows your Scroll Bar / Navigation settings.

### Folders
Folders can now show a single chosen image as their icon instead of the 2×2 app-contents preview (issue #57).  
The folder customization popup now mirrors the app icon popup: an "Icon" picker on the right of the preview and a "Reset" button on the left.

### Performance & Fixes
Added an app-list cache to the drawer to fix the occasional "spin of death" / freeze on open (issue #63).  
Moved shaped-icon generation off the main thread to smooth out high-refresh-rate devices (issue #63).  
The drawer swipe keeps animating even when system animations are disabled.  
Fixed apps reshuffling when the drawer opens/closes, and the home screen briefly appearing even when that page was removed.  
Fixed a UI freeze / blank screen when moving from the Manage Tab Settings screen back to Settings.
