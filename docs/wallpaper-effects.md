# Wallpaper Editor — Effect Algorithms

Reference for every adjustment in the wallpaper editor. The editor ships with
**9 scalar sliders + Crop**. Each slider is bipolar `-100..+100` with `0` =
neutral; `amt = slider / 100` maps to `-1..+1`.

## Implementation

- **Bake**: `app/src/main/java/com/bearinmind/launcher314/helpers/WallpaperHelper.kt`
  - `renderEditedBitmap()` — pipeline orchestrator
  - `applyPerPixelEffects()` — fused Loop A (Exposure, Contrast, Highlights, Shadows, Brightness)
  - `applyUnsharpMasks()` — Sharpness
  - `buildExposureLut()`, `buildContrastLut()`, `computeKelvinGains()`, `smoothstep()` — helpers
- **Live preview**: `app/src/main/java/com/bearinmind/launcher314/ui/settings/WallpaperEditorScreen.kt` → `previewColorFilter` (ColorMatrix-only, GPU-fused, 60 fps)

## Pipeline Order (bake)

```
1. Crop bitmap
2. Canvas draw with pre-draw ColorMatrix (GPU-fused):
   Temperature × Tint × Saturation × Filter
3. Loop A (one getPixels / setPixels):
   Exposure → Contrast → Highlights → Shadows → Brightness
4. Sharpness pass (one blur + one pixel loop)
5. Vignette (radial gradient, bake-only — not a user slider)
6. Background blur (stack blur, bake-only)
```

---

## Apps × Algorithms — Side-by-Side Math

Columns are implementations; rows are effects. Each cell shows the core math
formula used by that implementation. Cells marked **—** mean the app doesn't
ship that adjustment.

| Effect | **Ours** | GIMP/GEGL | GPUImage | Darktable | T8RIN/ImageToolbox | LavenderPhotos | ReFra |
|---|---|---|---|---|---|---|---|
| **Brightness** | `RGB += amt · 127` (sRGB-additive) | `(in - 0.5) · contrast + brightness + 0.5` (linear light, combined w/ contrast) | `rgb + vec3(brightness)` | (no standalone; inside exposure module) | `Aire.brightness(bitmap, bias)` → `RGB += bias · 255` | `RGB += value · 127f` via ColorMatrix | `RGB += value · 255` via ColorMatrix |
| **Exposure** | `lin = pow(rgb/255, 2.2); lin *= pow(2, amt·2); rgb = pow(lin, 1/2.2)·255` (LUT) | — | `rgb · pow(2.0, exposure)` (in sRGB gamma — **not linear light**) | `out_lin = (in_lin - black) · 1/(2^(-exposure) - black)` (linear light) | wraps GPUImage (sRGB-space) | — | — |
| **Contrast** | `gain = 1 + amt; LUT[i] = mean + (i-mean)·gain` (mean pivot, chroma-preserved by `RGB *= Y'/Y`) | `(in - 0.5) · contrast + 0.5` (fixed 0.5 pivot, linear light) | `(rgb - 0.5) · contrast + 0.5` | (via tonecurve module) | `Aire.contrast(bitmap, gain)` → scale around 0.5 | `c = (1+v)/(1.0001-v); offset = 0.5·(1-c)·255` (non-linear slider map) | ColorMatrix: `v·RGB + 128·(1-v)` |
| **Highlights** | `mask = smoothstep(0.5, 1, Y); delta = -amt·0.4·mask·Y·255; RGB += delta` | — (use curves) | `hi = clamp(1 - (pow(1-Y, 1/(2-h))·0.2 - 0.8·pow(1-Y, 2/(2-h))) - Y, -1, 0); out = (Y+hi+sh)/Y · rgb` | per-luminance tone-curve | wide-range variant of GPUImage shader | `out = rgb · (1 - highlight)` (global scale — no mask!) | — |
| **Shadows** | `mask = 1 - smoothstep(0, 0.5, Y); delta = amt·0.4·mask·(1-Y)·255; RGB += delta` | — | `sh = clamp(pow(Y, 1/(s+1)) - 0.76·pow(Y, 2/(s+1)) - Y, 0, 1)` | `RGB *= shTonecurve[Y]` | same wide-range shader | — (BlackPoint = flat additive) | — |
| **Saturation** | `ColorMatrix.setSaturation(1 + amt)` (Rec709) | `gray = dot(rgb, luma); out = gray + (rgb - gray) · scale` | `mix(vec3(lum), rgb, sat)` with Rec709 luma | per-pixel chroma scale | `Aire.saturation(bitmap, sat, tonemap)` | `ColorMatrix.setToSaturation(value + 1)` | explicit matrix w/ 0.213/0.715/0.072 |
| **Tint** (magenta↔green) | YIQ Q-axis: `R += 0.621·t; G -= 0.647·t; B += 1.702·t` | — (use channel mixer) | `yiq.Q += tint · 0.0523; RGB = YIQ→RGB(yiq)` | (part of white balance module) | wraps GPUImage WhiteBalance | color-gradient blend (not an axis) | — |
| **Temperature** | Tanner Helland Kelvin gains `K=6500+amt·3000`, sum-normalized | — | soft-light blend with warm overlay (not Kelvin) | `XYZ blackbody` via camera matrix | wraps GPUImage | Tanner Helland Kelvin gains, **no normalization** | — |
| **Sharpness** | `blur = stackBlur(r=4); out = src + amt·(src - blur)` | unsharp mask w/ threshold | 5-tap cross: `center·(1+4s) - edges·s` | `out = in + amt · max(\|in-blur\|-thr, 0) · sign(in-blur)` | wraps GPUImage / `Aire.unsharp` | — | `Aire.sharpness(bitmap, kernelSize)` |

---

## Per-Effect Verdict + Closest Reference

| Effect | Our math | Closest reference | Verdict |
|---|---|---|---|
| **Brightness** | sRGB-additive `RGB += amt·127` | LavenderPhotos (identical) | Match — mainstream |
| **Exposure** | Linearize → `×2^(amt·2)` → delinearize | Darktable `exposure.c` | **Better than Android consensus** — we're the only Android impl doing it in linear light |
| **Contrast** | Mean-pivoted linear gain with chroma preservation | GIMP/GPUImage (same math, 128 pivot instead of mean) | Match — mean pivot is our one custom twist, keeps dark images from uniformly darkening at +contrast |
| **Highlights** | `smoothstep(0.5,1,Y)` + Y scaling | GPUImage `GPUImageHighlightShadowFilter` | Match — simpler than GPUImage's pow-stack, same effect |
| **Shadows** | Inverse smoothstep + `(1-Y)` scaling | GPUImage, RawTherapee | Match |
| **Saturation** | `ColorMatrix.setSaturation(1+amt)` | GIMP, GPUImage, LavenderPhotos, ReFra | Match — identical math everywhere |
| **Tint** | YIQ Q-axis shift | GPUImage `GPUImageWhiteBalanceFilter` | Match (after we fixed the I↔Q axis bug) |
| **Temperature** | Tanner Helland Kelvin gains, sum-normalized | LavenderPhotos (same formula, unnormalized) | Match — the sum-normalization is our addition to prevent overall brightness drift |
| **Sharpness** | Small-radius unsharp mask `src + amt·(src - blur(r=4))` | Darktable `sharpen.c`, GPUImage | Match — textbook UM |

**Scorecard**: 9 / 9 match canonical references. Zero known bugs. Exposure is *strictly more correct* than the algorithm T8RIN / ImageToolbox ships (they inherit GPUImage's sRGB-space shortcut).

---

## YIQ → RGB Reference (Tint math)

```
R = Y + 0.956·I + 0.621·Q
G = Y − 0.272·I − 0.647·Q
B = Y − 1.105·I + 1.702·Q
```

- **I-axis** (orange ↔ cyan) — what Temperature does
- **Q-axis** (magenta ↔ green) — what Tint does

## Rec709 Luminance

`Y = 0.2126·R + 0.7152·G + 0.0722·B`

Used by Saturation, Contrast (mean & chroma preserve), Highlights, Shadows.

## Tanner Helland Kelvin → RGB

For `k = kelvin / 100`:

- `k ≤ 66`: `R = 255`, `G = 99.4708·ln(k) − 161.12`, `B = (k ≤ 19) ? 0 : 138.518·ln(k − 10) − 305.04`
- `k > 66`: `R = 329.699·(k − 60)^−0.1332`, `G = 288.122·(k − 60)^−0.0756`, `B = 255`

Clamp each to `[0,1]`. Normalize so sum = 3.

Source: https://tannerhelland.com/2012/09/18/convert-temperature-rgb-algorithm-code.html

---

## Preview vs Bake Drift

| Effect | Preview |
|---|---|
| Brightness | exact |
| Exposure | skips linearization — slightly gentler |
| Contrast | 128 pivot instead of image-mean — slight |
| Highlights | global contrast skew (bake uses smoothstep mask) — large at extremes |
| Shadows | same — large at extremes |
| Saturation | exact |
| Tint | exact |
| Temperature | exact |
| Sharpness | bake-only |

Preview stays ColorMatrix-only for 60 fps drag response. Acceptable trade-off;
could be mitigated with a debounced canonical re-render on a thumbnail.

---

## Reference Source Files

| Reference | File |
|---|---|
| GIMP/GEGL Brightness+Contrast | https://github.com/GNOME/gegl/blob/master/operations/common/brightness-contrast.c |
| GIMP/GEGL Saturation | https://github.com/GNOME/gegl/blob/master/operations/common/saturation.c |
| Darktable Exposure | https://github.com/darktable-org/darktable/blob/master/src/iop/exposure.c |
| Darktable Sharpen | https://github.com/darktable-org/darktable/blob/master/src/iop/sharpen.c |
| Darktable Temperature | https://github.com/darktable-org/darktable/blob/master/src/iop/temperature.c |
| RawTherapee tone curves | https://github.com/Beep6581/RawTherapee/blob/dev/rtengine/improcfun.cc |
| GPUImage Brightness | https://github.com/cats-oss/android-gpuimage/blob/master/library/src/main/java/jp/co/cyberagent/android/gpuimage/filter/GPUImageBrightnessFilter.java |
| GPUImage Exposure | https://github.com/cats-oss/android-gpuimage/blob/master/library/src/main/java/jp/co/cyberagent/android/gpuimage/filter/GPUImageExposureFilter.java |
| GPUImage Contrast | https://github.com/cats-oss/android-gpuimage/blob/master/library/src/main/java/jp/co/cyberagent/android/gpuimage/filter/GPUImageContrastFilter.java |
| GPUImage Saturation | https://github.com/cats-oss/android-gpuimage/blob/master/library/src/main/java/jp/co/cyberagent/android/gpuimage/filter/GPUImageSaturationFilter.java |
| GPUImage HighlightShadow | https://github.com/cats-oss/android-gpuimage/blob/master/library/src/main/java/jp/co/cyberagent/android/gpuimage/filter/GPUImageHighlightShadowFilter.java |
| GPUImage WhiteBalance | https://github.com/cats-oss/android-gpuimage/blob/master/library/src/main/java/jp/co/cyberagent/android/gpuimage/filter/GPUImageWhiteBalanceFilter.java |
| GPUImage Sharpen | https://github.com/cats-oss/android-gpuimage/blob/master/library/src/main/java/jp/co/cyberagent/android/gpuimage/filter/GPUImageSharpenFilter.java |
| T8RIN / ImageToolbox filters | https://github.com/T8RIN/ImageToolbox/tree/master/feature/filters/src/main/java/com/t8rin/imagetoolbox/feature/filters/data/model |
| T8RIN / wide-range H/S | https://github.com/T8RIN/ImageToolbox/blob/master/feature/filters/src/main/java/com/t8rin/imagetoolbox/feature/filters/data/utils/gpu/GPUImageHighlightShadowWideRangeFilter.kt |
| LavenderPhotos adjustments | https://github.com/kaii-lb/LavenderPhotos/blob/main/app/src/main/java/com/kaii/photos/helpers/editing/Shared.kt |
| ReFra (Gallery) adjustments | https://github.com/IacobIonut01/ReFra/tree/main/app/src/main/kotlin/com/dot/gallery/feature_node/presentation/edit/adjustments/varfilter |
| Aire native sharpness | https://github.com/awxkee/aire/blob/master/aire/src/main/cpp/base/Sharpness.cpp |
| Tanner Helland Kelvin→RGB | https://tannerhelland.com/2012/09/18/convert-temperature-rgb-algorithm-code.html |

---

## What You Could Add Next

Ideas ranked by payoff-vs-effort, scoped to the CPU / 8-bit-sRGB Android bake.

### High value, low effort

1. **Vignette slider** — the bake already computes it; just needs a pager entry + slider wiring. One of the highest-visible wallpaper effects; present in every editor.
2. **Vibrance** — chroma-weighted saturation that pushes unsaturated pixels more and saturated pixels less, with optional skin-tone protection near hue 25°. Distinct from Saturation. Every major editor ships it (Lightroom, T8RIN's `VibranceFilter`, Aire). Math:
   ```
   s = max(r,g,b) − min(r,g,b)        // current chroma 0..1
   weight = 1 − s                      // unsaturated pixels get more
   sat = setSaturation(1 + amt · weight)
   ```

### High value, medium effort

3. **Grain / film noise** — deterministic Gaussian noise masked to midtones. Ships in ImageToolbox, every Instagram-style editor, and VSCO. Math:
   ```
   seed = hash(x, y)                   // deterministic so bakes are reproducible
   n = gaussian(seed) · amt · 15       // ±15 LSB at +100
   luma = 0.299R + 0.587G + 0.114B
   rgb += n · smoothstep(0.15, 0.85, luma/255)
   ```
4. **Fade** — retro-film "faded" look: lifts blacks, compresses contrast, mild warm cast. Math: tonal LUT mapping `0 → amt·20, 255 → 255`, combined with a `+5K` temperature shift.
5. **Vignette color + feathering** — upgrade the existing vignette from "corner darkening only" to choose color + feathering stop:
   ```
   d = length((x/W - 0.5, y/H - 0.5))
   t = smoothstep(startRadius, endRadius, d)
   rgb = mix(rgb, vignetteColor, t · strength)
   ```

### Higher effort, smaller payoff

6. **Hue rotation** — full 360° rotation of the chroma plane (sin/cos on IQ). Lightroom treats Hue as a per-color-band slider so this would be less pro but simpler.
7. **Curves** — per-channel or luminance LUT editor; biggest UX + implementation cost, only worth it if advanced users are a target.
8. **Dehaze** — dark-channel-prior haze removal; T8RIN delegates to Aire native. Pure Kotlin is slow but feasible on small thumbnails.

### Polish / correctness

9. **Ratio-preserving Highlights/Shadows** — swap our Y-delta formula for GPUImage's `result = Y_new · rgb / Y_old`. Prevents desaturation of bright reds / greens near pure white / black. Same perf cost.
10. **Debounced canonical preview** — recompute the full bake on a 256×256 thumbnail ~150 ms after slider release, cross-fade into the live preview. Eliminates preview-vs-bake drift for Exposure / Highlights / Shadows / Contrast without hurting drag performance.
11. **Parallelize per-pixel loops** — split `pixels[0..N]` into 4 coroutines, ~3× bake speedup on modern phones.
12. **Unsharp threshold** (Darktable-style): skip amplification where `|src - blur| < threshold` — prevents noise amplification on Sharpness +100.
