package com.bearinmind.launcher314.helpers

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

object IconShapes {
    const val CIRCLE = "circle"
    const val ROUNDED_SQUARE = "rounded_square"
    const val SQUIRCLE = "squircle"
    const val TEARDROP = "teardrop"
    const val SAMMY = "sammy"
    const val CUPERTINO = "cupertino"

    val ALL = listOf(CIRCLE, ROUNDED_SQUARE, SQUIRCLE, TEARDROP, SAMMY, CUPERTINO)

    val LABELS = mapOf(
        CIRCLE to "Circle",
        ROUNDED_SQUARE to "Rounded",
        SQUIRCLE to "Squircle",
        TEARDROP to "Teardrop",
        SAMMY to "Sammy",
        CUPERTINO to "iOS"
    )
}

fun getIconShape(shapeName: String?): Shape? {
    return when (shapeName) {
        IconShapes.CIRCLE -> CircleShape
        IconShapes.ROUNDED_SQUARE -> RoundedCornerShape(8.dp)
        IconShapes.SQUIRCLE -> SquircleShape
        IconShapes.TEARDROP -> TeardropShape
        IconShapes.SAMMY -> SammyShape
        IconShapes.CUPERTINO -> CupertinoShape
        else -> null
    }
}

fun parseBlendMode(name: String?): BlendMode {
    return when (name) {
        "SrcAtop" -> BlendMode.SrcAtop
        "SrcIn" -> BlendMode.SrcIn
        "Multiply" -> BlendMode.Multiply
        "Screen" -> BlendMode.Screen
        "Overlay" -> BlendMode.Overlay
        else -> BlendMode.SrcAtop
    }
}

// Bezier constant for circular arc approximation
private const val ROUND_CD = 0.44777152f

// Builds a shape path using cubic Bezier corners.
// controlDistanceX/Y: 0.0 = sharp square, ROUND_CD = circle, values in between = squircle variants
private fun buildBezierShapePath(
    w: Float, h: Float,
    cdX: Float, cdY: Float = cdX
): Path {
    val hw = w / 2f
    val hh = h / 2f
    return Path().apply {
        // Start at top center
        moveTo(hw, 0f)
        // Top-right corner
        cubicTo(hw + hw * (1f - cdX), 0f, w, hh - hh * (1f - cdY), w, hh)
        // Bottom-right corner
        cubicTo(w, hh + hh * (1f - cdY), hw + hw * (1f - cdX), h, hw, h)
        // Bottom-left corner
        cubicTo(hw - hw * (1f - cdX), h, 0f, hh + hh * (1f - cdY), 0f, hh)
        // Top-left corner
        cubicTo(0f, hh - hh * (1f - cdY), hw - hw * (1f - cdX), 0f, hw, 0f)
        close()
    }
}

// Squircle — Bezier controlDistance=0.2 (matches Lawnchair/Neo Launcher)
private val SquircleShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(buildBezierShapePath(size.width, size.height, 0.2f))
    }
}

private val TeardropShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r = w * 0.35f
        val path = Path().apply {
            moveTo(w, 0f)
            lineTo(w, h - r)
            arcTo(rect = Rect(w - 2 * r, h - 2 * r, w, h), startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false)
            lineTo(r, h)
            arcTo(rect = Rect(0f, h - 2 * r, 2 * r, h), startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false)
            lineTo(0f, r)
            arcTo(rect = Rect(0f, 0f, 2 * r, 2 * r), startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false)
            close()
        }
        return Outline.Generic(path)
    }
}

// Samsung One UI — asymmetric Bezier (matches Lawnchair: X=0.4431717, Y=0.14010102)
private val SammyShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(buildBezierShapePath(size.width, size.height, 0.4431717f, 0.14010102f))
    }
}

// iOS Cupertino — pre-computed Bezier points from Lawnchair source (continuous curvature superellipse)
private val CupertinoShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            // Pre-computed control points from Lawnchair's Cupertino shape
            // Normalized to [0,1], scaled to actual size
            // Top edge → top-right corner
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.6039f, 0f, w * 0.712f, 0f, w * 0.83f, w * 0.0342f)
            cubicTo(w * 0.83f, w * 0.0342f, w * 0.9658f, w * 0.17f, w * 1f, w * 0.17f)
            // Right edge → bottom-right corner
            cubicTo(w * 1f, h * 0.288f, w * 1f, h * 0.3961f, w * 1f, h * 0.5f)
            cubicTo(w * 1f, h * 0.6039f, w * 1f, h * 0.712f, w * 0.9658f, h * 0.83f)
            cubicTo(w * 0.9658f, h * 0.83f, w * 0.83f, h * 0.9658f, w * 0.83f, h * 0.9658f)
            // Bottom edge → bottom-left corner
            cubicTo(w * 0.712f, h * 1f, w * 0.6039f, h * 1f, w * 0.5f, h * 1f)
            cubicTo(w * 0.3961f, h * 1f, w * 0.288f, h * 1f, w * 0.17f, h * 0.9658f)
            cubicTo(w * 0.17f, h * 0.9658f, w * 0.0342f, h * 0.83f, w * 0.0342f, h * 0.83f)
            // Left edge → top-left corner
            cubicTo(0f, h * 0.712f, 0f, h * 0.6039f, 0f, h * 0.5f)
            cubicTo(0f, h * 0.3961f, 0f, h * 0.288f, w * 0.0342f, h * 0.17f)
            cubicTo(w * 0.0342f, h * 0.17f, w * 0.17f, w * 0.0342f, w * 0.17f, w * 0.0342f)
            cubicTo(w * 0.288f, 0f, w * 0.3961f, 0f, w * 0.5f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
