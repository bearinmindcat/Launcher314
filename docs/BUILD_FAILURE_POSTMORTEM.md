# Silent Build Failures During the 2026-04-28 Debug Session

## Symptoms

- Multiple `./gradlew.bat assembleDebug` runs reported `BUILD SUCCESSFUL` to the user, but on inspection the APK file timestamp at `app/build/outputs/apk/debug/app-debug.apk` was **stuck at 2026-04-27 07:23** — the last time a *real* build had succeeded.
- `adb install -r` of that APK then reported `Performing Streamed Install / Success` and `dumpsys package` showed an updated `lastUpdateTime`, masking the fact that the device kept receiving the same stale binary.
- The launcher therefore kept exhibiting yesterday's behavior even after dozens of `Edit → build → install → reproduce` cycles. Toast probes, breadcrumb files, and `Log.e` lines added in source were all absent at runtime — because none of them were ever in the APK.

## Root cause

The build was failing at the Kotlin compile step with:

```
e: .../ui/widgets/WidgetManager.kt:307:32 Unresolved reference: setActivityResumed
```

`appWidgetHost.setActivityResumed(boolean)` is an API 35 method. The project sets `compileSdk = 35` in `app/build.gradle.kts:10`, but the **Android Gradle Plugin in use is 8.2.0** — which Gradle warns about explicitly:

```
WARNING: We recommend using a newer Android Gradle plugin to use compileSdk = 35
This Android Gradle plugin (8.2.0) was tested up to compileSdk = 34.
```

AGP 8.2.0 doesn't fully expose the API 35 SDK stubs to the Kotlin compiler, so a *direct* method reference to `setActivityResumed` fails at compile time even though the project's `compileSdk` is high enough.

## Why the failure was invisible

Two compounding factors:

1. **The build agent only captured the last 5–10 lines of Gradle output** (`./gradlew.bat assembleDebug 2>&1 | tail -5`). The compile error printed many lines before the failure summary; the tail truncation hid the error and showed only the `BUILD FAILED in 15s` summary line. When that line happened to be off-screen, the agent treated `exit code 0` (which Gradle does **not** return on failure — but the wrapper returned `0` because the agent's tail was reading from the success-cache log of a prior run in some cases) as "build succeeded."
2. **`adb install -r` and `dumpsys package` both still report `Success` / a fresh `lastUpdateTime` when the APK file you point them at is the same APK that's already on the device.** Android doesn't compare contents — it just reinstalls bytes. Without checking the APK file's modification time on the host, there's nothing in the install pipeline that says "the binary you're shipping is the same one you shipped yesterday."

The combined effect: every "build → install" cycle looked successful in the chat, the source on disk reflected the new edits, but the device kept running yesterday's compiled bytecode.

## Detection

The mismatch surfaced when a Toast that was unconditionally added to a tap handler **never appeared on screen**, AND a side-effect file write (`remove_screen_breadcrumb.txt`) **never showed up** in the app's private files directory, AND `logcat -d -s RemoveScreen:*` returned nothing — all three independent signals saying the new code wasn't running.

The smoking gun was running `ls -la app/build/outputs/apk/debug/app-debug.apk` and seeing the modify time was over 24 hours stale, despite many `BUILD SUCCESSFUL` messages.

## Fix

Replaced the direct method call with a runtime reflection lookup so the Kotlin compiler doesn't need to resolve the API 35 symbol at compile time:

```kotlin
// Before (broken under AGP 8.2.0 + compileSdk 35)
fun setActivityResumed(resumed: Boolean) {
    if (Build.VERSION.SDK_INT >= 35) {
        appWidgetHost?.setActivityResumed(resumed)   // ← unresolved
    }
}

// After
fun setActivityResumed(resumed: Boolean) {
    if (Build.VERSION.SDK_INT < 35) return
    val host = appWidgetHost ?: return
    try {
        val method = host.javaClass.getMethod("setActivityResumed", java.lang.Boolean.TYPE)
        method.invoke(host, resumed)
    } catch (_: Throwable) { /* device doesn't have it */ }
}
```

The method still no-ops on older platforms via the SDK_INT guard. On API 35+ devices the reflection finds the real method and invokes it.

## Lessons / preventive measures for future debug sessions

1. **Always tail or grep the FULL Gradle output for `BUILD FAILED`, not just the last few lines.** The build agent's `tail -5` heuristic must not be trusted alone.
2. **Check the APK file's modification timestamp on the host before installing.** A one-line sanity check: `ls -la app/build/outputs/apk/debug/app-debug.apk` should be inspected after every build.
3. **A diagnostic side-effect file (like the breadcrumb file) is the gold standard for "did my new code actually run."** Logcat and Toast can both be misread; a file in the app's private dir cannot.
4. **Plan an AGP upgrade.** The root cause is AGP 8.2.0 lagging behind `compileSdk = 35`. Upgrading to AGP 8.5+ would let API 35 calls compile directly, eliminating the need for the reflection workaround. The warning printed at every build start is real — treat it as a deferred to-do, not noise.

## Affected commits / files

- The reflection workaround landed in `app/src/main/java/com/bearinmind/launcher314/ui/widgets/WidgetManager.kt:setActivityResumed`.
- The original direct call had been part of the Launcher3-parity widget audit (see commit `ce030e8` "widget refresh cycle matches that of ASOP Launcher 3 and their respective forks…"). That commit was net-correct on devices with newer AGP, but bricked the dev build in this project's AGP 8.2.0 setup.
