package com.bearinmind.launcher314.helpers

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import com.bearinmind.launcher314.data.WP_FILTER_GRAYSCALE
import com.bearinmind.launcher314.data.WP_FILTER_INVERT
import com.bearinmind.launcher314.data.WP_FILTER_SEPIA
import com.bearinmind.launcher314.data.bumpWallpaperCacheVersion
import com.bearinmind.launcher314.data.getCustomWallpaperFile
import com.bearinmind.launcher314.data.getDeviceWallpaperSourceFile
import com.bearinmind.launcher314.data.setCustomWallpaperPath
import com.bearinmind.launcher314.data.setDeviceWallpaperSourcePath
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Helpers for importing a user-selected image as the launcher's custom wallpaper.
 * Images are downsampled to fit within the device resolution (so huge photos
 * don't waste memory) and saved into app-private storage as JPEG.
 */
object WallpaperHelper {
    private const val JPEG_QUALITY = 92

    /**
     * Imports [uri] into app-private storage as the custom wallpaper.
     * Returns the absolute file path on success, null on failure.
     */
    fun importCustomWallpaper(context: Context, uri: Uri): String? {
        return try {
            val displayMetrics = context.resources.displayMetrics
            val maxDim = maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels) * 2

            // Decode bounds first so we can compute sample size without OOM
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            val sampleSize = computeSampleSize(origW, origH, maxDim)

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap: Bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val outFile = getCustomWallpaperFile(context)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            bitmap.recycle()
            setCustomWallpaperPath(context, outFile.absolutePath)
            bumpWallpaperCacheVersion(context)
            outFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("WallpaperHelper", "Failed to import wallpaper", e)
            null
        }
    }

    fun clearCustomWallpaper(context: Context) {
        val f = getCustomWallpaperFile(context)
        if (f.exists()) f.delete()
        setCustomWallpaperPath(context, null)
        bumpWallpaperCacheVersion(context)
    }

    private fun computeSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxDim && h / 2 >= maxDim) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    /** Decode the saved wallpaper into a bitmap for rendering. Returns null if absent. */
    fun loadCustomWallpaperBitmap(context: Context): Bitmap? {
        val f = getCustomWallpaperFile(context)
        if (!f.exists()) return null
        return try {
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        } catch (e: Exception) {
            null
        }
    }

    // ========== Device wallpaper (Step 2) ==========

    /**
     * Imports [uri] as the editor's SOURCE image (the unedited version is kept so the
     * user can come back and re-edit). Saves to a separate file from the overlay mode's
     * custom wallpaper — the two modes are independent.
     */
    fun importDeviceWallpaperSource(context: Context, uri: Uri): String? {
        return try {
            val displayMetrics = context.resources.displayMetrics
            val maxDim = maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels) * 2

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            val sampleSize = computeSampleSize(origW, origH, maxDim)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap: Bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val outFile = getDeviceWallpaperSourceFile(context)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            bitmap.recycle()
            setDeviceWallpaperSourcePath(context, outFile.absolutePath)
            bumpWallpaperCacheVersion(context)
            outFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("WallpaperHelper", "Failed to import device wallpaper source", e)
            null
        }
    }

    fun clearDeviceWallpaperSource(context: Context) {
        val f = getDeviceWallpaperSourceFile(context)
        if (f.exists()) f.delete()
        setDeviceWallpaperSourcePath(context, null)
        bumpWallpaperCacheVersion(context)
    }

    /**
     * Renders the edited wallpaper bitmap for commit. Applies all transforms (scale,
     * offset, rotation, flip) and filters (brightness, contrast, saturation, named
     * filter, blur, vignette) into a single pass onto a device-sized bitmap.
     */
    fun renderEditedBitmap(
        context: Context,
        source: Bitmap,
        outputWidth: Int,
        outputHeight: Int,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotationDegrees: Int,
        flipH: Boolean,
        flipV: Boolean,
        cropLeft: Float = 0f,
        cropTop: Float = 0f,
        cropRight: Float = 1f,
        cropBottom: Float = 1f,
        brightness: Int,
        contrast: Int,
        saturation: Int,
        blur: Int,
        vignette: Int,
        filter: String,
        // Newer Samsung-Photos-style effect params. All -50..+50 unless noted.
        // Applied via color matrix in the same bake pass as brightness/contrast.
        // Sharpness (0..100) is post-processed with a convolution kernel.
        lightBalance: Int = 0,
        exposure: Int = 0,
        highlights: Int = 0,
        shadows: Int = 0,
        tint: Int = 0,
        temperature: Int = 0,
        sharpness: Int = 0,
        definition: Int = 0
    ): Bitmap {
        // Apply crop rectangle first — extract a sub-bitmap in image coordinates.
        val src: Bitmap = if (cropLeft > 0.001f || cropTop > 0.001f || cropRight < 0.999f || cropBottom < 0.999f) {
            val l = (cropLeft.coerceIn(0f, 1f) * source.width).toInt().coerceIn(0, source.width - 1)
            val t = (cropTop.coerceIn(0f, 1f) * source.height).toInt().coerceIn(0, source.height - 1)
            val r = (cropRight.coerceIn(0f, 1f) * source.width).toInt().coerceIn(l + 1, source.width)
            val b = (cropBottom.coerceIn(0f, 1f) * source.height).toInt().coerceIn(t + 1, source.height)
            Bitmap.createBitmap(source, l, t, r - l, b - t)
        } else source

        val out = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Build the transform matrix. Cover-scale so the cropped src fills at scale=1.
        val coverScale = maxOf(
            outputWidth.toFloat() / src.width,
            outputHeight.toFloat() / src.height
        )
        val userScale = scale.coerceAtLeast(0.1f)
        val finalScale = coverScale * userScale

        val matrix = Matrix().apply {
            // 1. scale around the source's own center so rotation orbits around that center too
            val sx = if (flipH) -finalScale else finalScale
            val sy = if (flipV) -finalScale else finalScale
            postScale(sx, sy, src.width / 2f, src.height / 2f)
            // 2. rotate around source center
            if (rotationDegrees != 0) {
                postRotate(rotationDegrees.toFloat(), src.width / 2f, src.height / 2f)
            }
            // 3. translate so source center lands at canvas center + user offset
            val boundsMatrix = Matrix(this)
            val bounds = android.graphics.RectF(0f, 0f, src.width.toFloat(), src.height.toFloat())
            boundsMatrix.mapRect(bounds)
            postTranslate(
                (outputWidth - bounds.width()) / 2f - bounds.left + offsetX,
                (outputHeight - bounds.height()) / 2f - bounds.top + offsetY
            )
        }

        // Build combined color matrix. All effect values are -100..+100 with
        // 0 as neutral; `amt(v) = v/100f` maps directly to a bipolar -1..+1
        // strength. Order mirrors the live preview composable.
        fun amt(v: Int): Float = v / 100f
        val combined = ColorMatrix()
        if (brightness != 0) combined.postConcat(makeBrightnessMatrix(brightness))
        if (exposure != 0) {
            val e = Math.pow(2.0, amt(exposure).toDouble()).toFloat()
            combined.postConcat(ColorMatrix(floatArrayOf(
                e, 0f, 0f, 0f, 0f,
                0f, e, 0f, 0f, 0f,
                0f, 0f, e, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        // lightBalance is applied AFTER the draw as a histogram-driven cubic LUT
        // (Apple patent US8314847B2 — Brilliance algorithm). Matrix-only version
        // was just a brightness shift, which isn't what users expect.
        if (temperature != 0) {
            val t = amt(temperature) * 40f
            combined.postConcat(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, t,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, -t,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (tint != 0) {
            val t = amt(tint) * 30f
            combined.postConcat(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, t * 0.5f,
                0f, 1f, 0f, 0f, -t,
                0f, 0f, 1f, 0f, t * 0.5f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        // highlights / shadows are applied AFTER the draw via per-pixel tone
        // curves (see below) — matrix-only versions affect every pixel equally,
        // which isn't what those sliders actually mean in photo editors.
        if (definition != 0) {
            val c = 1f + amt(definition) * 0.5f
            val t = 128f * (1f - c)
            combined.postConcat(ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (contrast != 0) combined.postConcat(makeContrastMatrix(contrast))
        if (saturation != 0) {
            val sat = ColorMatrix()
            sat.setSaturation((1f + amt(saturation)).coerceAtLeast(0f))
            combined.postConcat(sat)
        }
        when (filter) {
            WP_FILTER_GRAYSCALE -> {
                val g = ColorMatrix()
                g.setSaturation(0f)
                combined.postConcat(g)
            }
            WP_FILTER_SEPIA -> combined.postConcat(sepiaMatrix())
            WP_FILTER_INVERT -> combined.postConcat(invertMatrix())
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        if (!isIdentity(combined)) paint.colorFilter = ColorMatrixColorFilter(combined)
        canvas.drawBitmap(src, matrix, paint)
        // Recycle the intermediate cropped bitmap if we allocated one separate from source
        if (src !== source) src.recycle()

        // --- Per-pixel tonal passes that can't fit in a 4x5 ColorMatrix ---
        // LightBalance runs FIRST (it's a base auto-tone — Apple Brilliance
        // patent US8314847B2). Highlights/shadows/sharpness then fine-tune on
        // top of that corrected tone.
        applyLightBalance(out, lightBalance)
        applyTonalAndSharpness(out, highlights, shadows, sharpness)

        // Vignette — radial gradient darkening from corners inward
        if (vignette > 0) {
            val vp = Paint(Paint.ANTI_ALIAS_FLAG)
            val cx = outputWidth / 2f
            val cy = outputHeight / 2f
            val radius = maxOf(outputWidth, outputHeight) * 0.75f
            val alphaMax = (vignette / 100f * 0.85f).coerceIn(0f, 1f)
            vp.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((alphaMax * 0.4f * 255).toInt(), 0, 0, 0),
                    Color.argb((alphaMax * 255).toInt(), 0, 0, 0)
                ),
                floatArrayOf(0.4f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat(), vp)
        }

        return if (blur > 0) applyStackBlur(out, radiusPx = blur) else out
    }

    /**
     * Light Balance — histogram-driven cubic tone curve, implementing the
     * algorithm from Apple's patent US8314847B2 ("Automatic Tone Mapping Curve
     * Generation Based on Dynamically Stretched Image Histogram Distribution")
     * which underpins Apple Photos' Brilliance/Light slider.
     *
     * Steps (per the patent):
     *   1. Build a 256-bin luminance histogram (Rec709 Y).
     *   2. Histogram-stretch endpoints: drop the darkest 0.1% and brightest
     *      1% as outliers, so `low..high` contains the real image range.
     *   3. Inside that stretched range, count pixel mass in three zones
     *      (shadows < 0.25, midtones 0.25..0.75, highlights > 0.75).
     *   4. Derive endpoint slopes of an S-curve:
     *        S0 = clamp(1.2 - (midMass + lowMass),  0.1, 1.2)
     *        S1 = clamp(1.2 - (midMass + highMass), 0.1, 1.2)
     *      — higher slope at an endpoint lifts/compresses that side more.
     *   5. Fit a Hermite cubic through (0,0)→(1,1) using those slopes and bake
     *      into a 256-entry LUT.
     *   6. Apply the LUT per pixel, blended with identity by the slider amount.
     *
     * Slider maps 0..100 to blend -1..+1. Positive blends toward the full
     * algorithmic curve; negative blends toward a mild midgray compression
     * (a de-contrasted inverse). 50 = identity (no change).
     */
    private fun applyLightBalance(out: Bitmap, lightBalance: Int) {
        if (lightBalance == 0) return
        val amount = lightBalance / 100f  // -1..+1

        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        // Step 1: Rec709 luminance histogram.
        val histogram = IntArray(256)
        for (p in pixels) {
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b + 0.5f).toInt().coerceIn(0, 255)
            histogram[lum]++
        }
        val total = w * h

        // Step 2: Find 0.1% / top-1% percentile cutoffs for the stretch.
        val lowCut = (total * 0.001f).toInt().coerceAtLeast(1)
        val highCut = (total * 0.01f).toInt().coerceAtLeast(1)
        var low = 0
        var cum = 0
        for (i in 0..255) { cum += histogram[i]; if (cum >= lowCut) { low = i; break } }
        var high = 255
        cum = 0
        for (i in 255 downTo 0) { cum += histogram[i]; if (cum >= highCut) { high = i; break } }
        val stretchRange = (high - low).coerceAtLeast(1).toFloat()

        // Step 3: Zone masses after stretching each histogram bin to 0..1.
        var lowMass = 0
        var midMass = 0
        var highMass = 0
        for (i in 0..255) {
            val stretched = ((i - low) / stretchRange).coerceIn(0f, 1f)
            val count = histogram[i]
            when {
                stretched < 0.25f -> lowMass += count
                stretched < 0.75f -> midMass += count
                else -> highMass += count
            }
        }
        val lowFrac = lowMass.toFloat() / total
        val midFrac = midMass.toFloat() / total
        val highFrac = highMass.toFloat() / total

        // Step 4: Endpoint slopes.
        val s0 = (1.2f - (midFrac + lowFrac)).coerceIn(0.1f, 1.2f)
        val s1 = (1.2f - (midFrac + highFrac)).coerceIn(0.1f, 1.2f)

        // Step 5: Build 256-entry LUT. Hermite cubic with y0=0, y1=1, m0=s0, m1=s1:
        //   H(t) = t^3*(s0 + s1 - 2) + t^2*(-2s0 - s1 + 3) + t*s0
        val lut = IntArray(256)
        for (i in 0..255) {
            val t = ((i - low) / stretchRange).coerceIn(0f, 1f)
            val curved = t * t * t * (s0 + s1 - 2f) + t * t * (-2f * s0 - s1 + 3f) + t * s0
            val curved8 = (curved * 255f).coerceIn(0f, 255f)
            val blended = if (amount >= 0f) {
                // Blend identity → patent curve.
                i * (1f - amount) + curved8 * amount
            } else {
                // Negative side: compress toward midgray (128) for a muted/flat look.
                i * (1f + amount) + 128f * (-amount)
            }
            lut[i] = blended.toInt().coerceIn(0, 255)
        }

        // Step 6: Apply LUT per channel. Patent applies to luminance only, but
        // per-channel is the cheap equivalent used by most mobile editors and
        // matches how the rest of our pipeline operates on RGB directly.
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xff
            val r = lut[(p shr 16) and 0xff]
            val g = lut[(p shr 8) and 0xff]
            val b = lut[p and 0xff]
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * Per-pixel tonal adjustments that a 4x5 ColorMatrix can't express:
     * - Highlights: shift only pixels above midtone luminance.
     * - Shadows:    shift only pixels below midtone luminance.
     * - Sharpness:  unsharp mask — out = out + amt * (out - blur(out)).
     *
     * All three share a single IntArray pixel pass so the expensive
     * getPixels/setPixels round-trip happens once.
     */
    private fun applyTonalAndSharpness(out: Bitmap, highlights: Int, shadows: Int, sharpness: Int) {
        val hAmt = if (highlights != 0) highlights / 100f else 0f
        val sAmt = if (shadows != 0) shadows / 100f else 0f
        val sharpAmt = if (sharpness != 0) sharpness / 100f else 0f
        if (hAmt == 0f && sAmt == 0f && sharpAmt == 0f) return

        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        // For sharpness we need a blurred copy. Reuse applyStackBlur with a
        // small radius so the high-frequency detail shows up in the subtract.
        val blurPixels: IntArray? = if (sharpAmt != 0f) {
            val blurred = applyStackBlur(out.copy(out.config ?: Bitmap.Config.ARGB_8888, true), radiusPx = 4)
            val bp = IntArray(w * h)
            blurred.getPixels(bp, 0, w, 0, 0, w, h)
            blurred.recycle()
            bp
        } else null

        // Max luminance shift at ±1 amount. ~80 gives a visible but not
        // nuclear highlights/shadows response; adjust to taste later.
        val maxShift = 80f

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xff
            var r = (p shr 16) and 0xff
            var g = (p shr 8) and 0xff
            var b = p and 0xff

            // Rec709 luminance 0..1
            val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f

            // Soft linear masks that peak at the extremes and hit 0 at 0.5.
            // (A smoothstep would be nicer but linear is 3× cheaper per pixel
            // over a 1080x2400 wallpaper and the difference is subtle.)
            if (hAmt != 0f || sAmt != 0f) {
                val hMask = ((lum - 0.5f) * 2f).coerceIn(0f, 1f)
                val sMask = ((0.5f - lum) * 2f).coerceIn(0f, 1f)
                val delta = hAmt * hMask * maxShift + sAmt * sMask * maxShift
                if (delta != 0f) {
                    r = (r + delta).toInt().coerceIn(0, 255)
                    g = (g + delta).toInt().coerceIn(0, 255)
                    b = (b + delta).toInt().coerceIn(0, 255)
                }
            }

            // Unsharp mask for sharpness. Negative amount = soft blur blend.
            if (sharpAmt != 0f && blurPixels != null) {
                val bp = blurPixels[i]
                val br = (bp shr 16) and 0xff
                val bg = (bp shr 8) and 0xff
                val bb = bp and 0xff
                r = (r + sharpAmt * (r - br)).toInt().coerceIn(0, 255)
                g = (g + sharpAmt * (g - bg)).toInt().coerceIn(0, 255)
                b = (b + sharpAmt * (b - bb)).toInt().coerceIn(0, 255)
            }

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun makeBrightnessMatrix(brightness: Int): ColorMatrix {
        // -100..+100 with 0 = neutral → offset -127..+127
        val b = brightness / 100f * 127f
        return ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    private fun makeContrastMatrix(contrast: Int): ColorMatrix {
        // -100..+100 with 0 = neutral → scale 0.5..1.5 around midpoint 128
        val c = 1f + contrast / 100f * 0.5f
        val t = 128f * (1f - c)
        return ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    private fun sepiaMatrix(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f,     0f,     0f,     1f, 0f
    ))

    private fun invertMatrix(): ColorMatrix = ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    ))

    private fun isIdentity(cm: ColorMatrix): Boolean {
        val arr = cm.array
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        for (i in arr.indices) if (kotlin.math.abs(arr[i] - identity[i]) > 1e-4f) return false
        return true
    }

    /**
     * Commits [bitmap] as the actual Android system wallpaper.
     * [targetFlags] is a bitwise OR of WallpaperManager.FLAG_SYSTEM / FLAG_LOCK.
     * Returns true on success.
     */
    fun applyAsSystemWallpaper(context: Context, bitmap: Bitmap, targetFlags: Int): Boolean {
        val wm = WallpaperManager.getInstance(context)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(bitmap, null, true, targetFlags)
            } else {
                @Suppress("DEPRECATION")
                wm.setBitmap(bitmap)
            }
            true
        } catch (e: IOException) {
            android.util.Log.e("WallpaperHelper", "Failed to set system wallpaper", e)
            false
        } catch (e: SecurityException) {
            android.util.Log.e("WallpaperHelper", "Missing SET_WALLPAPER permission", e)
            false
        }
    }

    /**
     * Stack-blur approximation (in-place) — lightweight, CPU-only, no RenderScript.
     * Good enough for wallpaper baking since the output is viewed at low zoom.
     */
    private fun applyStackBlur(bitmap: Bitmap, radiusPx: Int): Bitmap {
        if (radiusPx < 1) return bitmap
        // Simple repeated box blur — much faster than gaussian, visually similar
        // when applied 2-3 passes. We downscale to speed things up, then upscale.
        val downscale = 4
        val w = bitmap.width / downscale
        val h = bitmap.height / downscale
        if (w <= 0 || h <= 0) return bitmap
        val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val scaledRadius = (radiusPx.toFloat() / downscale).coerceAtLeast(1f).toInt()

        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        repeat(2) { boxBlur(pixels, w, h, scaledRadius) }
        small.setPixels(pixels, 0, w, 0, 0, w, h)

        val final = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
        small.recycle()
        if (final !== bitmap) bitmap.recycle()
        return final
    }

    private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val tmp = IntArray(pixels.size)
        val div = (radius * 2 + 1).coerceAtLeast(1)
        // Horizontal pass
        for (y in 0 until height) {
            var r = 0; var g = 0; var b = 0; var a = 0
            for (x in -radius..radius) {
                val xx = x.coerceIn(0, width - 1)
                val p = pixels[y * width + xx]
                a += (p ushr 24) and 0xFF
                r += (p ushr 16) and 0xFF
                g += (p ushr 8) and 0xFF
                b += p and 0xFF
            }
            for (x in 0 until width) {
                tmp[y * width + x] = ((a / div) shl 24) or ((r / div) shl 16) or ((g / div) shl 8) or (b / div)
                val outX = (x - radius).coerceIn(0, width - 1)
                val inX = (x + radius + 1).coerceIn(0, width - 1)
                val pOut = pixels[y * width + outX]
                val pIn = pixels[y * width + inX]
                a += ((pIn ushr 24) and 0xFF) - ((pOut ushr 24) and 0xFF)
                r += ((pIn ushr 16) and 0xFF) - ((pOut ushr 16) and 0xFF)
                g += ((pIn ushr 8) and 0xFF) - ((pOut ushr 8) and 0xFF)
                b += (pIn and 0xFF) - (pOut and 0xFF)
            }
        }
        // Vertical pass
        for (x in 0 until width) {
            var r = 0; var g = 0; var b = 0; var a = 0
            for (y in -radius..radius) {
                val yy = y.coerceIn(0, height - 1)
                val p = tmp[yy * width + x]
                a += (p ushr 24) and 0xFF
                r += (p ushr 16) and 0xFF
                g += (p ushr 8) and 0xFF
                b += p and 0xFF
            }
            for (y in 0 until height) {
                pixels[y * width + x] = ((a / div) shl 24) or ((r / div) shl 16) or ((g / div) shl 8) or (b / div)
                val outY = (y - radius).coerceIn(0, height - 1)
                val inY = (y + radius + 1).coerceIn(0, height - 1)
                val pOut = tmp[outY * width + x]
                val pIn = tmp[inY * width + x]
                a += ((pIn ushr 24) and 0xFF) - ((pOut ushr 24) and 0xFF)
                r += ((pIn ushr 16) and 0xFF) - ((pOut ushr 16) and 0xFF)
                g += ((pIn ushr 8) and 0xFF) - ((pOut ushr 8) and 0xFF)
                b += (pIn and 0xFF) - (pOut and 0xFF)
            }
        }
    }
}
