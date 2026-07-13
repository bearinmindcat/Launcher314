# Release Notes for v0.0.20-beta

## What's New

Added drawer tabs — organize your apps into custom categories (tabs) in the app drawer, Neo/Nova style.

Added a "Manage Tab Settings" screen (under the app drawer preview in Settings) to create, edit, delete, and reorder tabs, plus options for "Hide added apps from all", "Swipe between tabs", "Show app count", "Hide (+) in drawer", and a Left/Center/Right tab alignment slider.  
Tabs can be locked with a password — a locked tab prompts for the password before it opens, before it can be edited, and again when saving changes to it.  
The app drawer preview in Settings now shows the tab chips, styled to match the real drawer (outline, alignment, count, and hide-(+) all reflected live).  
Delete popups (delete tab / delete folder) now share one consistent style with an outlined light-red Delete button and a confirmation prompt.  
Fixed an error where moving from the Manage Tab Settings screen back to Settings could freeze the UI or show a blank screen; navigation and back handling are now guarded against rapid taps.  
Improved performance of the settings preview so it renders without stalling.  
Updated the long-press popup animation (app icons, widgets, folders) — the menu now cleanly pops out of the item on open and shrinks back into it on close.
