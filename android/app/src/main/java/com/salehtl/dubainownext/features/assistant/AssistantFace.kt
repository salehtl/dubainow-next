package com.salehtl.dubainownext.features.assistant

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.components.DNLogo
import kotlin.math.abs
import kotlin.math.sin

/** The light the assistant is made of: the brand blues (Paper's conic edge/ring gradient). */
internal object AssistantLight {
    val ring = listOf(DNColor.blue300, DNColor.blue600, DNColor.blue200, DNColor.blue800, DNColor.blue400, DNColor.blue700, DNColor.blue300)
    val glow = DNColor.actionAccent
    val line = DNColor.blue200
    val halo = DNColor.blue300
}

/**
 * Its face: the DubaiNow mark in a white tonal orb (Android: no glass), with a ring of light that
 * says what it's doing (Paper W1D-0 · eight states, one mark): ripples listening, a comet thinking,
 * a progress arc working, light gathering at the bottom on your turn, the ring closing and glowing once
 * when done, a still soft ring when quiet. 160 dp box; the orb is 68 dp, the ring 74 dp.
 */
@Composable
internal fun AssistantFace(
    state: AssistantFaceState,
    progress: Float,
    awake: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val wake by animateFloatAsState(if (awake) 1f else 0.6f, DNMotion.bouncy(), label = "wake")
    val wakeAlpha by animateFloatAsState(if (awake) 1f else 0f, tween(300), label = "wakeAlpha")
    val arc by animateFloatAsState(progress.coerceIn(0.02f, 1f), DNMotion.smooth(), label = "arc")

    val infinite = rememberInfiniteTransition(label = "face")
    val comet by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "comet")

    // Listening: a running clock for the two ripples (read only while drawing).
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state == AssistantFaceState.Listening && !reduceMotion) {
        if (state != AssistantFaceState.Listening || reduceMotion) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { clock.floatValue = (it - start) / 1_000_000_000f }
    }

    // Your turn: one nod towards the button under your thumb. Done: the ring glows once (0.6 s).
    val nod = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (reduceMotion) return@LaunchedEffect
        when (state) {
            AssistantFaceState.YourTurn -> { nod.animateTo(8f, DNMotion.bouncy()); nod.animateTo(0f, DNMotion.bouncy()) }
            AssistantFaceState.Done -> { glow.animateTo(1f, tween(300)); glow.animateTo(0f, tween(300)) }
            else -> Unit
        }
    }

    Box(
        // One element for TalkBack ("DubaiNow assistant, Working on it"); the logo inside is decorative.
        modifier.size(160.dp).clearAndSetSemantics {
            contentDescription = "DubaiNow assistant"
            stateDescription = state.spoken
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            // Halo: a soft light from the logo.
            drawCircle(
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.95f), 0.42f to DNColor.blue50.copy(alpha = 0.78f),
                    0.72f to AssistantLight.halo.copy(alpha = 0.32f), 1f to AssistantLight.halo.copy(alpha = 0f),
                    center = c, radius = 80.dp.toPx(),
                ),
                radius = 80.dp.toPx(), center = c, alpha = if (state == AssistantFaceState.Quiet) 0.6f else 1f,
            )
            if (state == AssistantFaceState.Listening && !reduceMotion) drawRipples(clock.floatValue)
            drawRing(state, arc, comet, glow.value, wake, wakeAlpha, reduceMotion)
        }
        // The orb: white and tonal (Android has no glass), with a soft blue shadow.
        Box(
            Modifier.size(68.dp)
                .graphicsLayer { scaleX = wake; scaleY = wake; alpha = wakeAlpha }
                .shadow(10.dp, CircleShape, ambientColor = DNColor.blue800, spotColor = DNColor.blue800)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF3F5FF))), CircleShape),
        )
        DNLogo(
            Modifier.graphicsLayer {
                val s = 0.74f + 0.26f * ((wake - 0.6f) / 0.4f).coerceIn(0f, 1.2f)
                scaleX = s; scaleY = s
                rotationX = nod.value
                cameraDistance = 12f * density
            },
            height = 38.dp,
        )
    }
}

private fun DrawScope.drawRipples(t: Float) {
    val level = 0.5f + 0.5f * abs(sin(t * 5.3f) * sin(t * 2.1f + 0.6f))
    val level2 = 0.5f + 0.5f * abs(sin(t * 3.7f + 1.1f) * sin(t * 1.7f))
    val r1 = 66.dp.toPx() * (0.86f + 0.16f * level2)
    drawCircle(DNColor.blue300.copy(alpha = 0.06f), r1)
    drawCircle(DNColor.blue600.copy(alpha = 0.16f), r1, style = Stroke(1.dp.toPx()))
    val r2 = 49.dp.toPx() * (0.9f + 0.14f * level)
    drawCircle(DNColor.blue300.copy(alpha = 0.10f), r2)
    drawCircle(DNColor.blue600.copy(alpha = 0.34f), r2, style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.drawRing(
    state: AssistantFaceState, arc: Float, comet: Float, glow: Float, wake: Float, wakeAlpha: Float, reduceMotion: Boolean,
) {
    val r = 37.dp.toPx() * wake
    val c = center
    fun glowDisc(alpha: Float, extra: Float, dy: Float = 0f) {
        val gr = r + extra.dp.toPx()
        val gc = Offset(c.x, c.y + dy.dp.toPx())
        drawCircle(
            Brush.radialGradient(0.55f to AssistantLight.glow.copy(alpha = alpha * wakeAlpha), 1f to AssistantLight.glow.copy(alpha = 0f), center = gc, radius = gr),
            radius = gr, center = gc,
        )
    }
    fun conicFill() = rotate(200f) { drawCircle(Brush.sweepGradient(AssistantLight.ring, c), r, alpha = wakeAlpha) }
    val stroke = 3.5.dp.toPx()
    val arcInset = r - stroke / 2
    val arcTopLeft = Offset(c.x - arcInset, c.y - arcInset)
    val arcSize = Size(arcInset * 2, arcInset * 2)

    when (state) {
        AssistantFaceState.Resting -> Unit
        AssistantFaceState.Awake, AssistantFaceState.Listening -> {
            glowDisc(if (state == AssistantFaceState.Listening) 0.38f else 0.30f, if (state == AssistantFaceState.Listening) 14f else 10f)
            conicFill()
        }
        AssistantFaceState.Thinking -> {
            glowDisc(0.22f, 9f)
            drawCircle(DNColor.blue100, r, alpha = wakeAlpha)
            if (reduceMotion) {
                drawCircle(DNColor.blue400, r - 1.5.dp.toPx(), style = Stroke(3.dp.toPx()))
            } else {
                rotate(comet) {
                    drawArc(
                        Brush.sweepGradient(0f to DNColor.blue600.copy(alpha = 0f), 0.3f to DNColor.blue600, 0.3001f to Color.Transparent, 1f to Color.Transparent, center = c),
                        startAngle = 0f, sweepAngle = 108f, useCenter = false, topLeft = arcTopLeft, size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        AssistantFaceState.Working -> {
            // The ring fills as steps finish, matching the steps on the card.
            glowDisc(0.25f, 9f)
            drawCircle(DNColor.blue100, r, alpha = wakeAlpha)
            rotate(-90f) {
                drawArc(
                    Brush.sweepGradient(listOf(DNColor.blue400, DNColor.blue600, DNColor.blue700), c),
                    startAngle = 0f, sweepAngle = 360f * arc, useCenter = false, topLeft = arcTopLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        AssistantFaceState.YourTurn -> {
            // The light gathers at the bottom, towards the button under your thumb.
            glowDisc(0.36f, 12f, dy = 8f)
            drawCircle(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), DNColor.blue200.copy(alpha = 0.85f), DNColor.blue600), startY = c.y - r, endY = c.y + r),
                r, alpha = wakeAlpha,
            )
        }
        AssistantFaceState.Done -> {
            // The ring closes and glows once.
            glowDisc(0.30f + 0.30f * glow, 10f + 12f * glow)
            conicFill()
            rotate(-90f) {
                drawArc(DNColor.blue600, 0f, 360f * arc, false, arcTopLeft, arcSize, style = Stroke(stroke * 0.6f, cap = StrokeCap.Round), alpha = 0.55f)
            }
        }
        AssistantFaceState.Quiet -> drawCircle(DNColor.blue200.copy(alpha = 0.8f), r, alpha = wakeAlpha)
    }
}
