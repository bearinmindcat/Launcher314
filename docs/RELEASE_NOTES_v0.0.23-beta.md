# Release Notes for v0.0.23-beta

## What's New

Added a Direct dial 1x1 widget — the new Launcher314 section at the top of the Widgets screen has it; pick a contact and a single tap on the icon calls them (falls back to a pre-filled dialer if the call permission is denied) - issue #78

Added a "Hide home screen apps" toggle to Additional Drawer Settings — apps you place on the home screen automatically disappear from the drawer and come back when removed; search and custom tabs still show them - issue #79

Added Pinned apps — a card in Additional Drawer Settings opens a Hide-Apps-style picker where apps AND drawer folders can be pinned to the top of the drawer, and the pinned chips can be held and dragged to set your own order (the drawer follows it)

Added a Drawer noise slider (0-100%) to Additional Drawer Settings for the frosted-glass grain in the drawer background

Back and Home buttons now close an open folder instead of doing nothing — works for home screen, dock and drawer folders (drawer sub-folders still step out one level at a time) - issue #80

Icon packs can now be applied to shortcut icons (browser / Website Shortcut items) through the customize popup, same as regular apps - issue #85

Renaming an app now shows everywhere — the drawer displays, sorts and searches by the custom label instead of ignoring it - issue #87

Fixed folder customization (Shape / Outline / Size) not applying — drawer, home and dock folders now honor per-folder settings - issue #75

Fixed the "Hide source badge" option not working on shortcut icons inside folders (per-app customizations were dropped in folder popups) - issue #82

Fixed folders showing the label "Folder" instead of their real name while dragging an app over them - issue #77

Fixed a crash on devices with multi-activity apps (duplicate rows in Hide Apps / app picker lists) - issue #74
