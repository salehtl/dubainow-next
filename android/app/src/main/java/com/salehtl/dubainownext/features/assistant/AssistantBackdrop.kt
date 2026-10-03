package com.salehtl.dubainownext.features.assistant

import android.graphics.Matrix
import android.graphics.SweepGradient
import android.os.Build
import android.view.RoundedCorner
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor

/**
 * Everything between the face and your screen (Paper V83-0 · "Three zones, one light"), Android
 * version: no glass or blur. A soft veil so the screen reads as "behind" (it stays visible and
 * live), a wash of light from the logo, a frost on your side (the bottom), and the animated blue
 * edge light around the screen. Tapping the veil closes the assistant (or dismisses the keyboard).
 * [bottomBand]: how far up from the screen's bottom edge your side reaches (chips, caption,
 * transcript, the bar), so the frost can sit nearly solid behind it.
 */
@Composable
internal fun AssistantBackdrop(edge: Float, reduceMotion: Boolean, bottomBand: Dp, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onTap() } }) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            // Veil: light, as on iOS (0.22), so the screen stays readable and live underneath
            // (Paper V83-0: "the edge light and a soft wash from the logo are the only change").
            drawRect(DNColor.surfaceCanvas.copy(alpha = 0.22f))
            drawRect(DNColor.blue600.copy(alpha = 0.05f))
            // A soft wash from the logo (ellipse 170% × 84% centred on the top edge).
            val r = h * 0.42f
            withTransform({ scale(scaleX = (w * 1.7f) / (h * 0.84f), scaleY = 1f, pivot = Offset(w / 2, 0f)) }) {
                drawCircle(
                    Brush.radialGradient(listOf(DNColor.blue300.copy(alpha = 0.42f), DNColor.blue300.copy(alpha = 0f)), center = Offset(w / 2, 0f), radius = r),
                    radius = r, center = Offset(w / 2, 0f),
                )
            }
            // Your side: frost behind the suggestions and the ask bar.
            val frost = 360.dp.toPx()
            drawRect(
                Brush.verticalGradient(
                    0f to DNColor.surfaceCanvas.copy(alpha = 0f), 0.45f to DNColor.surfaceCanvas.copy(alpha = 0.66f), 1f to DNColor.surfaceCanvas.copy(alpha = 0.9f),
                    startY = h - frost, endY = h,
                ),
                topLeft = Offset(0f, h - frost),
            )
            // iOS blurs this zone (ultraThinMaterial); without blur the screen's own text stayed sharp
            // under the caption and transcript. So behind your side the frost turns nearly solid
            // (≈ 0.96 with the layer above), fading in over 64 dp just above it.
            if (bottomBand > 0.dp) {
                val solid = h - (bottomBand + 12.dp).toPx()
                val fade = 64.dp.toPx()
                drawRect(
                    Brush.verticalGradient(
                        0f to DNColor.surfaceCanvas.copy(alpha = 0f), 1f to DNColor.surfaceCanvas.copy(alpha = 0.85f),
                        startY = solid - fade, endY = solid,
                    ),
                    topLeft = Offset(0f, solid - fade),
                )
            }
        }
        AssistantEdgeLight(edge, reduceMotion)
    }
}

/**
 * The edge light: a sweep of the brand blues travelling round the screen edge (one turn / 9 s),
 * breathing ±8 % every 2.4 s, over a fine 2 dp line. Glow is built from layered strokes, so it
 * works on every API level. Reduce Motion ("Remove animations"): a still 2 dp line.
 */
@Composable
internal fun AssistantEdgeLight(intensity: Float, reduceMotion: Boolean) {
    val corner = rememberAssistantScreenCorner()
    val infinite = rememberInfiniteTransition(label = "edge")
    val turn by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "turn")
    val breathe by infinite.animateFloat(0.84f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "breathe")
    val colors = remember { AssistantLight.ring.map { it.toArgb() }.toIntArray() }
    val matrix = remember { Matrix() }

    Canvas(Modifier.fillMaxSize()) {
        val cr = CornerRadius(corner.toPx())
        if (!reduceMotion && intensity > 0f) {
            val shader = SweepGradient(size.width / 2, size.height / 2, colors, null)
            matrix.setRotate(turn, size.width / 2, size.height / 2)
            shader.setLocalMatrix(matrix)
            val brush = ShaderBrush(shader)
            // Many faint strokes of growing width make a smooth falloff from the edge (no blur
            // needed). Visible depth is half of each stroke: the outer half falls off the screen.
            val steps = 18
            val a = 0.045f * breathe * intensity
            for (k in 0 until steps) {
                val width = (4f + k * 4f).dp.toPx()
                drawRoundRect(brush, cornerRadius = cr, style = Stroke(width), alpha = a)
            }
            for (k in 0 until 6) {
                drawRoundRect(AssistantLight.glow, cornerRadius = cr, style = Stroke((6f + k * 6f).dp.toPx()), alpha = 0.03f * intensity)
            }
        }
        if (intensity > 0f || reduceMotion) {
            drawRoundRect(AssistantLight.line.copy(alpha = 0.9f), cornerRadius = cr, style = Stroke(4.dp.toPx()))
        }
    }
}

/** The display's corner radius (API 31+), so the edge light follows the glass; 28 dp otherwise. */
@Composable
private fun rememberAssistantScreenCorner(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    return remember(view) {
        val px = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius
        } else null
        if (px != null && px > 0) with(density) { px.toDp() } else 28.dp
    }
}

