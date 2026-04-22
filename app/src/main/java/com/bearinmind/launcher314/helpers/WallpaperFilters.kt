package com.bearinmind.launcher314.helpers

import com.bearinmind.launcher314.data.WP_FILTER_COOL
import com.bearinmind.launcher314.data.WP_FILTER_FADED
import com.bearinmind.launcher314.data.WP_FILTER_GRAYSCALE
import com.bearinmind.launcher314.data.WP_FILTER_INVERT
import com.bearinmind.launcher314.data.WP_FILTER_KODACHROME
import com.bearinmind.launcher314.data.WP_FILTER_NIGHT
import com.bearinmind.launcher314.data.WP_FILTER_POLAROID
import com.bearinmind.launcher314.data.WP_FILTER_SEPIA
import com.bearinmind.launcher314.data.WP_FILTER_TEAL_ORANGE
import com.bearinmind.launcher314.data.WP_FILTER_VINTAGE
import com.bearinmind.launcher314.data.WP_FILTER_VIVID
import com.bearinmind.launcher314.data.WP_FILTER_WARM

/**
 * Named preset ColorMatrix filters — shared between the live-preview
 * ColorFilter chain in the editor and the bake-time pipeline in
 * WallpaperHelper. Each entry is a 4×5 matrix in Android's row-major R/G/B/A
 * layout with the offset column already in 0..255 scale.
 *
 * Matrices sourced from the widely-copied Pixi.js `ColorMatrixFilter`
 * catalog (which itself derives from Evan Wallace / Mario Klingemann /
 * Brad Larson prior work) and BT.709 luminance-weighted grayscale.
 *
 * All filters here are pure ColorMatrix — no LUT, no convolution — so they
 * compose cleanly with the rest of our pre-draw matrix and run essentially
 * free on the GPU for live previews.
 */
object WallpaperFilters {

    /** High-contrast B&W using BT.709 weights scaled 1.25× with a black-point shift. */
    val MONO = floatArrayOf(
        0.45f, 0.88f, 0.15f, 0f, -32f,
        0.45f, 0.88f, 0.15f, 0f, -32f,
        0.45f, 0.88f, 0.15f, 0f, -32f,
        0f,    0f,    0f,    1f,  0f
    )

    /** Classic sepia (Microsoft's published matrix). */
    val SEPIA = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f,     0f,     0f,     1f, 0f
    )

    /** Color inversion. */
    val INVERT = floatArrayOf(
        -1f, 0f,  0f,  0f, 255f,
         0f, -1f, 0f,  0f, 255f,
         0f,  0f, -1f, 0f, 255f,
         0f,  0f,  0f, 1f, 0f
    )

    /** Warm "sunny" shift: R up, B down, slight global lift. */
    val WARM = floatArrayOf(
        1.10f, 0f,    0f,    0f, 10f,
        0f,    1.02f, 0f,    0f, 0f,
        0f,    0f,    0.90f, 0f, -8f,
        0f,    0f,    0f,    1f, 0f
    )

    /** Cool "arctic" shift: B up, R down. */
    val COOL = floatArrayOf(
        0.90f, 0f,    0f,    0f, -8f,
        0f,    1.00f, 0f,    0f, 0f,
        0f,    0f,    1.12f, 0f, 12f,
        0f,    0f,    0f,    1f, 0f
    )

    /** Saturation 1.5 × contrast 1.15 pre-combined. */
    val VIVID = floatArrayOf(
         1.274f, -0.286f, -0.041f, 0f, -19.2f,
        -0.105f,  1.453f, -0.195f, 0f, -19.2f,
        -0.105f, -0.286f,  1.544f, 0f, -19.2f,
         0f,      0f,      0f,     1f,   0f
    )

    /** Muted / faded: desaturate ~40% + lift blacks (film-stock look). */
    val FADED = floatArrayOf(
        0.8f, 0.1f, 0.1f, 0f, 15f,
        0.1f, 0.8f, 0.1f, 0f, 15f,
        0.1f, 0.1f, 0.8f, 0f, 15f,
        0f,   0f,   0f,   1f,  0f
    )

    /** Pixi.js `polaroid()` — cross-processed film pastel. */
    val POLAROID = floatArrayOf(
         1.438f, -0.122f, -0.016f, 0f, -7.65f,
        -0.062f,  1.378f, -0.016f, 0f,  5.10f,
        -0.062f, -0.122f,  1.483f, 0f, -5.10f,
         0f,      0f,      0f,     1f,   0f
    )

    /** Pixi.js `kodachrome()` — rich red / deep blue cinematic film look. */
    val KODACHROME = floatArrayOf(
         1.1286f, -0.3967f, -0.0399f, 0f, 63.73f,
        -0.1640f,  1.0835f, -0.0550f, 0f, 24.73f,
        -0.1679f, -0.5603f,  1.6015f, 0f,  8.69f,
         0f,       0f,       0f,      1f,  0f
    )

    /** Pixi.js `vintage()` — warm, faded, slight green cast. */
    val VINTAGE = floatArrayOf(
        0.6279f, 0.3202f, -0.0397f, 0f, 24.61f,
        0.0269f, 0.6420f,  0.0580f, 0f, 19.03f,
        0.1446f, 0.4491f,  0.5141f, 0f, 13.16f,
        0f,      0f,       0f,      1f,  0f
    )

    /** Cinematic teal-shadows / orange-highlights cross-mix. */
    val TEAL_ORANGE = floatArrayOf(
         1.05f, 0f,    -0.10f, 0f, 12f,
         0f,    1.00f, -0.05f, 0f, 0f,
        -0.10f, 0f,     1.00f, 0f, -10f,
         0f,    0f,     0f,    1f,  0f
    )

    /** Low-key darken + slight lift — AMOLED-friendly. */
    val NIGHT = floatArrayOf(
        0.4f, 0f,   0f,   0f, 25.5f,
        0f,   0.4f, 0f,   0f, 25.5f,
        0f,   0f,   0.4f, 0f, 25.5f,
        0f,   0f,   0f,   1f,  0f
    )

    /**
     * Resolves a saved filter key to its ColorMatrix floats, or `null`
     * for "none" / unknown. Consumers should pass the returned array
     * straight into `android.graphics.ColorMatrix(...)` or the
     * Compose-side `androidx.compose.ui.graphics.ColorMatrix(...)`.
     */
    fun matrixFor(filterKey: String?): FloatArray? = when (filterKey) {
        WP_FILTER_GRAYSCALE -> MONO
        WP_FILTER_SEPIA -> SEPIA
        WP_FILTER_INVERT -> INVERT
        WP_FILTER_WARM -> WARM
        WP_FILTER_COOL -> COOL
        WP_FILTER_VIVID -> VIVID
        WP_FILTER_FADED -> FADED
        WP_FILTER_POLAROID -> POLAROID
        WP_FILTER_KODACHROME -> KODACHROME
        WP_FILTER_VINTAGE -> VINTAGE
        WP_FILTER_TEAL_ORANGE -> TEAL_ORANGE
        WP_FILTER_NIGHT -> NIGHT
        else -> null
    }
}
