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
        // Samsung-Photos-style effect params. All -100..+100 with 0 = neutral.
        exposure: Int = 0,
        highlights: Int = 0,
        shadows: Int = 0,
        tint: Int = 0,
        temperature: Int = 0,
        sharpness: Int = 0
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

        // === Pre-draw ColorMatrix (GPU-fused with canvas draw) ===
        // Composes Temperature (Helland Kelvin gains) × Tint (YIQ Q-axis) ×
        // Saturation × Filter. Everything else is per-pixel (LUTs / masks /
        // unsharp masks) because it either depends on image statistics, needs
        // non-linear light, uses smoothstep masks, or requires blurred copies
        // — none of which fit in a 4×5 color matrix.
        fun amt(v: Int): Float = v / 100f
        val combined = ColorMatrix()
        if (temperature != 0) {
            val gains = computeKelvinGains(amt(temperature))
            combined.postConcat(ColorMatrix(floatArrayOf(
                gains[0], 0f, 0f, 0f, 0f,
                0f, gains[1], 0f, 0f, 0f,
                0f, 0f, gains[2], 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (tint != 0) {
            // YIQ Q-axis (magenta ↔ green) — third column of the YIQ→RGB
            // matrix, NOT the second (which is the I axis = orange↔cyan and
            // duplicates Temperature's behavior). Matches GPUImage's
            // WhiteBalanceFilter tint parameter.
            val t = amt(tint) * 30f
            combined.postConcat(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 0.621f * t,
                0f, 1f, 0f, 0f, -0.647f * t,
                0f, 0f, 1f, 0f, 1.702f * t,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (saturation != 0) {
            val sat = ColorMatrix()
            sat.setSaturation((1f + amt(saturation)).coerceAtLeast(0f))
            combined.postConcat(sat)
        }
        WallpaperFilters.matrixFor(filter)?.let { combined.postConcat(ColorMatrix(it)) }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        if (!isIdentity(combined)) paint.colorFilter = ColorMatrixColorFilter(combined)
        canvas.drawBitmap(src, matrix, paint)
        if (src !== source) src.recycle()

        // === Post-draw per-pixel pipeline ===
        // Loop A: Exposure (linearized ±2 stops) → Contrast (mean-pivoted
        //         linear gain, chroma-preserving) → Highlights (smoothstep
        //         + Y scaling) → Shadows (smoothstep + (1-Y) scaling) →
        //         Brightness (additive sRGB). One getPixels/setPixels pair.
        // Loop B: Sharpness (small-radius unsharp mask).
        applyPerPixelEffects(
            out,
            exposure, contrast, highlights, shadows, brightness
        )
        applyUnsharpMasks(out, sharpness)

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
     * Fused per-pixel pass: Exposure (linearized 2^stops), Contrast (mean-
     * pivoted linear gain with chroma preservation), Highlights (smoothstep
     * mask on top 1/3 of tones), Shadows (inverse smoothstep on bottom 1/3),
     * Brightness (additive in sRGB gamma space).
     *
     * Order matters: Exposure runs first in linear light; Contrast LUT then
     * acts on the exposed pixel; range-selective masks next; additive
     * Brightness last.
     *
     * Chroma preservation for Contrast scales RGB by Y' / Y so hue stays put
     * (channels clamp individually to 0..255 at extremes).
     */
    private fun applyPerPixelEffects(
        out: Bitmap,
        exposure: Int,
        contrast: Int,
        highlights: Int,
        shadows: Int,
        brightness: Int
    ) {
        if (exposure == 0 && contrast == 0 &&
            highlights == 0 && shadows == 0 && brightness == 0) return

        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        // Single-pass image mean for Contrast's mean-pivot.
        var lumSum = 0L
        val total = w * h
        if (contrast != 0) {
            for (p in pixels) {
                val r = (p shr 16) and 0xff
                val g = (p shr 8) and 0xff
                val b = p and 0xff
                val y = (0.2126f * r + 0.7152f * g + 0.0722f * b + 0.5f).toInt().coerceIn(0, 255)
                lumSum += y
            }
        }
        val mean = if (total > 0) lumSum.toFloat() / total else 128f

        val expLut: IntArray? = if (exposure != 0) buildExposureLut(exposure / 100f) else null
        val conLut: IntArray? = if (contrast != 0) buildContrastLut(mean, contrast / 100f) else null

        val hAmt = if (highlights != 0) highlights / 100f else 0f
        val sAmt = if (shadows != 0) shadows / 100f else 0f
        val brightAdd = if (brightness != 0) (brightness / 100f) * 127f else 0f

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xff
            var r = (p shr 16) and 0xff
            var g = (p shr 8) and 0xff
            var b = p and 0xff

            // 1. Exposure — LUT bakes in linearize → ×2^stops → delinearize.
            if (expLut != null) {
                r = expLut[r]; g = expLut[g]; b = expLut[b]
            }

            // 2. Contrast — mean-pivoted linear gain, chroma-preserving.
            if (conLut != null) {
                val y = (0.2126f * r + 0.7152f * g + 0.0722f * b + 0.5f).toInt().coerceIn(0, 255)
                val yp = conLut[y]
                if (y > 0) {
                    val ratio = yp.toFloat() / y
                    r = (r * ratio).toInt().coerceIn(0, 255)
                    g = (g * ratio).toInt().coerceIn(0, 255)
                    b = (b * ratio).toInt().coerceIn(0, 255)
                } else {
                    r = yp; g = yp; b = yp
                }
            }

            // 3/4. Highlights (smoothstep + Y scale) + Shadows (inverse + (1-Y)).
            if (hAmt != 0f || sAmt != 0f) {
                val yn = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
                val hMask = smoothstep(0.5f, 1.0f, yn)
                val sMask = 1f - smoothstep(0f, 0.5f, yn)
                val delta = (-hAmt * 0.4f * hMask * yn + sAmt * 0.4f * sMask * (1f - yn)) * 255f
                if (delta != 0f) {
                    r = (r + delta).toInt().coerceIn(0, 255)
                    g = (g + delta).toInt().coerceIn(0, 255)
                    b = (b + delta).toInt().coerceIn(0, 255)
                }
            }

            // 5. Brightness — additive in sRGB gamma space.
            if (brightAdd != 0f) {
                r = (r + brightAdd).toInt().coerceIn(0, 255)
                g = (g + brightAdd).toInt().coerceIn(0, 255)
                b = (b + brightAdd).toInt().coerceIn(0, 255)
            }

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * Sharpness — small-radius (4 px) unsharp mask: `out = src + amt·(src-blur)`.
     * Pixel-scale edge enhancement.
     */
    private fun applyUnsharpMasks(out: Bitmap, sharpness: Int) {
        if (sharpness == 0) return

        val w = out.width
        val h = out.height
        val config = out.config ?: Bitmap.Config.ARGB_8888

        val blurred = applyStackBlur(out.copy(config, true), radiusPx = 4)
        val blurPixels = IntArray(w * h)
        blurred.getPixels(blurPixels, 0, w, 0, 0, w, h)
        blurred.recycle()

        val sharpAmt = sharpness / 100f

        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xff
            var r = (p shr 16) and 0xff
            var g = (p shr 8) and 0xff
            var b = p and 0xff

            val bp = blurPixels[i]
            val br = (bp shr 16) and 0xff
            val bg = (bp shr 8) and 0xff
            val bb = bp and 0xff
            r = (r + sharpAmt * (r - br)).toInt().coerceIn(0, 255)
            g = (g + sharpAmt * (g - bg)).toInt().coerceIn(0, 255)
            b = (b + sharpAmt * (b - bb)).toInt().coerceIn(0, 255)

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * Exposure LUT — linearize → ×2^(amount·2) (±2 stops) → delinearize,
     * baked into 256 entries so the fused pixel loop does an array index
     * instead of three `pow` calls per channel per pixel.
     */
    private fun buildExposureLut(amount: Float): IntArray {
        val scale = Math.pow(2.0, (amount * 2.0)).toFloat()
        val lut = IntArray(256)
        for (i in 0..255) {
            val lin = Math.pow(i / 255.0, 2.2).toFloat()
            val lifted = (lin * scale).coerceIn(0f, 1f)
            val gamma = Math.pow(lifted.toDouble(), 1.0 / 2.2).toFloat()
            lut[i] = (gamma * 255f).toInt().coerceIn(0, 255)
        }
        return lut
    }

    /**
     * Contrast LUT — linear gain around the image's mean luminance. This is
     * the GIMP / GPUImage / GEGL convention, differing only in the pivot:
     * we use the image's mean instead of a fixed 128 so dark photos don't
     * uniformly darken under +contrast. `gain = 1 + amount` gives identity
     * at amt=0, clipping at ±1 (which is standard behavior).
     */
    private fun buildContrastLut(mean: Float, amount: Float): IntArray {
        val gain = 1f + amount
        val lut = IntArray(256)
        for (i in 0..255) {
            val y = (i - mean) * gain
            lut[i] = (mean + y).toInt().coerceIn(0, 255)
        }
        return lut
    }

    /**
     * Tanner Helland's Kelvin → RGB gain approximation, mapped from slider
     * −1..+1 into 3500 K..9500 K. Normalized so midgray stays midgray
     * (sum of gains = 3). Follows the Planckian locus of blackbody radiators
     * so cool ↔ warm shifts along the photographic white-balance axis
     * instead of a flat linear ±R∓B.
     *
     *  https://tannerhelland.com/2012/09/18/convert-temperature-rgb-algorithm-code.html
     *
     * Returns [rGain, gGain, bGain].
     */
    fun computeKelvinGains(amount: Float): FloatArray {
        val kelvin = 6500f + amount * 3000f
        val k = kelvin / 100f
        var r: Float; var g: Float; var b: Float
        if (kelvin <= 6600f) {
            r = 1f
            g = (0.390081579f * kotlin.math.ln(k.toDouble()).toFloat() - 0.631841444f).coerceIn(0f, 1f)
            b = if (kelvin <= 1900f) 0f else
                (0.543206789f * kotlin.math.ln((k - 10f).toDouble()).toFloat() - 1.19625408f).coerceIn(0f, 1f)
        } else {
            r = (1.29293618f * Math.pow((k - 60f).toDouble(), -0.1332047592).toFloat()).coerceIn(0f, 1f)
            g = (1.12989086f * Math.pow((k - 60f).toDouble(), -0.0755148492).toFloat()).coerceIn(0f, 1f)
            b = 1f
        }
        val sum = r + g + b
        if (sum > 0.001f) {
            val norm = 3f / sum
            r *= norm; g *= norm; b *= norm
        }
        return floatArrayOf(r, g, b)
    }

    private inline fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

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
