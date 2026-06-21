package com.bearinmind.launcher314.ui.home

/**
 * Tiny cross-composable flag: is the home-screen page pager currently scrolling
 * / settling (fling)? Set from LauncherScreen (which owns the PagerState) and
 * read imperatively by the drawer swipe gesture in LauncherWithDrawer.
 *
 * Why: right after a page swipe the pager is still settling and CONSUMES the
 * next touch to stop its fling. The drawer gesture normally defers to a child
 * that consumed (so it doesn't steal icon drags). That deference ate the first
 * swipe-up after a page change. By knowing the pager is settling, the drawer can
 * claim a clearly-vertical swipe immediately even though the pager consumed —
 * the Launcher3/Lawnchair "single arbiter decides by axis" behavior — while
 * still deferring to children when the pager is at rest.
 */
object HomePagerSwipeState {
    @Volatile
    var isSettling: Boolean = false
}
