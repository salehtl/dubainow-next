package com.salehtl.dubainownext.features.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.Inter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Shared building blocks for the Home widgets (Paper C1 · 02 Widgets · Anatomy and grid, AVV):
// glyph (34 dp city cue) · optional quick action (34 dp circle in a 44 dp target) ·
// hero figure (Inter 44/46, tabular) · unit (17/22) · caption (13/18) · optional meter (6 dp).
// Widgets are a Shared component: they look the same on both platforms, in Inter.

// MARK: - Locals

/** The quick action handler for the widget being drawn (null in previews: quick actions don't tap). */
internal val LocalWidgetQuick = compositionLocalOf<((WidgetQuickIntent) -> Unit)?> { null }

/** Where a widget's quick action is drawn, in the widget's own coordinates. */
internal data class WidgetQuickSlot(val owner: Any, val intent: WidgetQuickIntent, val bounds: Rect)

/**
 * Collects where the quick action is drawn so [WidgetView] can lay a TalkBack-only button over it: the
 * card itself reads one sentence (its drawn text is cleared), and the quick action stays its own
 * element, like the separate VoiceOver button iOS draws on top of the card.
 */
internal class WidgetQuickSlots {
    var card: LayoutCoordinates? = null
    var slot by mutableStateOf<WidgetQuickSlot?>(null)
        private set

    fun report(owner: Any, intent: WidgetQuickIntent, at: LayoutCoordinates) {
        val card = card ?: return
        if (!card.isAttached || !at.isAttached) return
        val next = WidgetQuickSlot(owner, intent, card.localBoundingBoxOf(at, clipBounds = false))
        if (next != slot) slot = next
    }

    fun clear(owner: Any) {
        if (slot?.owner === owner) slot = null
    }
}

/** The slots of the widget being drawn (null in previews). */
internal val LocalWidgetQuickSlots = compositionLocalOf<WidgetQuickSlots?> { null }

/** Reports this quick action's place to the widget (and forgets it when it goes). */
@Composable
private fun Modifier.widgetQuickSlot(intent: WidgetQuickIntent): Modifier {
    val slots = LocalWidgetQuickSlots.current ?: return this
    val owner = remember { Any() }
    DisposableEffect(slots, owner) { onDispose { slots.clear(owner) } }
    return onGloballyPositioned { slots.report(owner, intent, it) }
}

/** The app's launch moment: live values that tick in the demo (metro departures) count from here. */
internal object WidgetClock {
    val launch: LocalDateTime = LocalDateTime.now()
}

// MARK: - Type

/** A widget text style in Inter (sizes in sp; WidgetView caps the font scale like iOS caps Dynamic Type). */
internal fun widgetType(
    size: Float,
    weight: Int = 400,
    lineHeight: Float = size * 1.3f,
    tracking: Float = 0f,
    tabular: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = (tracking / size).em,
    fontFeatureSettings = if (tabular) "tnum" else null,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

// MARK: - Surfaces

/** What a widget is painted on. Inverse surfaces are only for things that are dark in the city. */
internal sealed interface WidgetSurface {
    data object Plain : WidgetSurface
    data object Inverse : WidgetSurface
    data object Board : WidgetSurface
    data object Sky : WidgetSurface
    data class OnHue(val hue: Hue) : WidgetSurface

    val isDark: Boolean get() = this == Inverse || this == Board
}

internal fun Modifier.widgetSurface(surface: WidgetSurface): Modifier = when (surface) {
    WidgetSurface.Plain -> background(DNColor.widgetSurface)
    WidgetSurface.Inverse -> background(DNColor.widgetSurfaceInverse)
    WidgetSurface.Board -> background(DNColor.widgetSurfaceBoard)
    WidgetSurface.Sky -> background(Brush.verticalGradient(listOf(DNColor.widgetSkyTop, DNColor.widgetSkyBottom)))
    is WidgetSurface.OnHue -> background(surface.hue.gradient)
}

// MARK: - Quick actions

/** What a quick action does. WidgetView maps each to a router call or a store action. */
internal sealed interface WidgetQuickIntent {
    data object ExtendParking : WidgetQuickIntent
    data object StartParking : WidgetQuickIntent
    data object TopUpSalik : WidgetQuickIntent
    data object TopUpNol : WidgetQuickIntent
    data object RenewFamilyVisas : WidgetQuickIntent
    /** Account name ("Salik", "DEWA", "nol"). */
    data class Connect(val account: String) : WidgetQuickIntent

    val label: String
        get() = when (this) {
            ExtendParking -> "Extend parking"
            StartParking -> "Start parking"
            TopUpSalik -> "Top up Salik"
            TopUpNol -> "Top up nol card"
            RenewFamilyVisas -> "Renew family visas"
            is Connect -> "Connect $account"
        }
}

internal enum class WidgetQuickTone(val fill: Color, val ink: Color) {
    Subtle(DNColor.actionAccentSubtle, DNColor.textLink),
    Filled(DNColor.actionAccent, DNColor.white),
    OnInverse(DNColor.widgetOnInverseTrack, DNColor.white),
    Destructive(DNColor.actionDestructive, DNColor.white),
    White(DNColor.white, DNColor.textPrimary),
}

/** The + circle quick action: 34 dp disc in a 44 dp target (Compose widens the touch area to 48). */
@Composable
internal fun WidgetQuickPlus(intent: WidgetQuickIntent, tone: WidgetQuickTone = WidgetQuickTone.Subtle) {
    val handler = LocalWidgetQuick.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.9f else 1f, DNMotion.snappy(), label = "quick-plus")
    Box(
        Modifier
            .size(44.dp)
            .widgetQuickSlot(intent)
            .let {
                if (handler == null) it.clearAndSetSemantics { }
                else it.clip(CircleShape)
                    .clickable(interaction, ripple(bounded = false, radius = 22.dp), role = Role.Button) { handler(intent) }
                    .semantics { contentDescription = intent.label }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).scale(s).clip(CircleShape).background(tone.fill), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = tone.ink, modifier = Modifier.size(20.dp))
        }
    }
}

/** A capsule quick action with a verb ("+ Extend", "Top up", "Connect"). */
@Composable
internal fun WidgetQuickCapsule(
    intent: WidgetQuickIntent,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: WidgetQuickTone = WidgetQuickTone.Subtle,
    height: Dp = 40.dp,
    fullWidth: Boolean = true,
) {
    val handler = LocalWidgetQuick.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, DNMotion.snappy(), label = "quick-capsule")
    val small = height < 34.dp
    Row(
        modifier
            .let { if (fullWidth) it.fillMaxWidth() else it }
            .height(height)
            .widgetQuickSlot(intent)
            .scale(s)
            .clip(CircleShape)
            .background(tone.fill)
            .let {
                if (handler == null) it.clearAndSetSemantics { }
                // TalkBack reads the full action ("Extend parking"), not just the verb on the capsule.
                else it.clickable(interaction, ripple(), role = Role.Button) { handler(intent) }
                    .semantics { contentDescription = intent.label }
            }
            .padding(horizontal = if (small) 12.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tone.ink, modifier = Modifier.size(16.dp))
        Text(
            title, style = widgetType(if (small) 13f else 15f, 600), color = tone.ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

// MARK: - Figures and captions

/** One part of a hero figure: a value and its trailing unit ("42" "min"). */
internal data class WidgetFigurePart(val value: String, val unit: String? = null)

/**
 * The hero figure with its unit. Currency leads ("AED 18"), time trails ("42 min").
 * Changed digits roll (each character animates on its own, like iOS `.numericText`).
 * Shrinks to 60 % when it doesn't fit, like `minimumScaleFactor(0.6)`.
 */
@Composable
internal fun WidgetFigure(
    parts: List<WidgetFigurePart>,
    modifier: Modifier = Modifier,
    leadingUnit: String? = null,
    color: Color = DNColor.textPrimary,
    unitColor: Color? = null,
    size: Float = 44f,
    numeric: Double = 0.0,
) {
    val valueStyle = widgetType(size, 700, lineHeight = size + 2f, tracking = if (size >= 40f) -0.8f else -0.3f, tabular = true)
    val unitStyle = widgetType(17f, 600, lineHeight = 22f)
    WidgetFit(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (leadingUnit != null) {
                Text(leadingUnit, style = unitStyle, color = unitColor ?: color, modifier = Modifier.alignByBaseline().padding(end = 1.dp))
            }
            parts.forEach { part ->
                WidgetRollingText(part.value, valueStyle, color, numeric, Modifier.alignByBaseline())
                if (part.unit != null) {
                    Text(part.unit, style = unitStyle, color = unitColor ?: color, modifier = Modifier.alignByBaseline().padding(end = 2.dp))
                }
            }
        }
    }
}

@Composable
internal fun WidgetFigure(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    leadingUnit: String? = null,
    color: Color = DNColor.textPrimary,
    unitColor: Color? = null,
    size: Float = 44f,
    numeric: Double = 0.0,
) = WidgetFigure(listOf(WidgetFigurePart(value, unit)), modifier, leadingUnit, color, unitColor, size, numeric)

/**
 * Text whose changed characters roll: up when [numeric] increases, down when it decreases.
 * Characters are keyed from the right so units, tens and hundreds keep their slots. The size and the
 * baselines come from a static copy of the text, so baseline alignment never follows a rolling digit.
 */
@Composable
internal fun WidgetRollingText(text: String, style: TextStyle, color: Color, numeric: Double, modifier: Modifier = Modifier) {
    val previous = remember { mutableDoubleStateOf(numeric) }
    val increasing = numeric >= previous.doubleValue
    SideEffect { previous.doubleValue = numeric }
    Layout(
        content = {
            Text(text, style = style, maxLines = 1, softWrap = false)
            Row {
                val chars = text.toList()
                chars.forEachIndexed { i, c ->
                    key(chars.size - i) {
                        AnimatedContent(
                            targetState = c,
                            transitionSpec = {
                                val dir = if (increasing) 1 else -1
                                (slideInVertically(DNMotion.smooth()) { h -> dir * h } + fadeIn(DNMotion.fast())) togetherWith
                                    (slideOutVertically(DNMotion.smooth()) { h -> -dir * h } + fadeOut(DNMotion.fast())) using
                                    SizeTransform(clip = false)
                            },
                            modifier = Modifier.clipToBounds(),
                            label = "roll",
                        ) { ch -> Text(ch.toString(), style = style, color = color, maxLines = 1, softWrap = false) }
                    }
                }
            }
        },
        // Read as text (not a description), so a card that merges it still reads only its own sentence.
        modifier = modifier.clearAndSetSemantics { this.text = AnnotatedString(text) },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val reference = measurables[0].measure(loose)
        val rolling = measurables[1].measure(loose)
        layout(
            maxOf(reference.width, rolling.width), maxOf(reference.height, rolling.height),
            mapOf(FirstBaseline to reference[FirstBaseline], LastBaseline to reference[LastBaseline]),
        ) { rolling.place(0, 0) }
    }
}

/** Measures its content at its natural width and scales it down (to [minScale]) when it doesn't fit. */
@Composable
internal fun WidgetFit(modifier: Modifier = Modifier, minScale: Float = 0.6f, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val p = measurables.first().measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
        val scale = if (constraints.hasBoundedWidth && p.width > constraints.maxWidth) {
            max(minScale, constraints.maxWidth.toFloat() / p.width)
        } else 1f
        val w = min((p.width * scale).roundToInt(), if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE)
        val h = (p.height * scale).roundToInt()
        layout(w, h) {
            p.placeWithLayer(0, 0) {
                scaleX = scale; scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

/**
 * 13/18 caption under the figure. With [minScale] below 1 it shrinks to fit before it truncates (iOS
 * `minimumScaleFactor`); the 18 line height stays, so the widget's layout doesn't move.
 */
@Composable
internal fun WidgetCaption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DNColor.textSecondary,
    weight: Int = 400,
    lines: Int = 1,
    minScale: Float = 1f,
) {
    val style = widgetType(13f, weight, lineHeight = 18f)
    if (minScale < 1f) {
        BasicText(
            text, modifier, style = style.copy(color = color), maxLines = lines, overflow = TextOverflow.Ellipsis,
            autoSize = WidgetShrinkToFit(min = (13f * minScale).sp, max = 13.sp),
        )
    } else {
        Text(text, modifier, style = style, color = color, maxLines = lines, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Full size whenever it fits, else the largest 0.25 sp step down to [min]. `StepBased` alone never lands on
 * [max] when the range isn't a whole number of steps (13 → 11.05 gives 12.8), so it's only asked once [max] overflows.
 */
private data class WidgetShrinkToFit(val min: TextUnit, val max: TextUnit) : TextAutoSize {
    private val steps = TextAutoSize.StepBased(minFontSize = min, maxFontSize = max, stepSize = 0.25.sp)

    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        val full = performLayout(constraints, text, max)
        val fits = !full.hasVisualOverflow && (full.lineCount == 0 || !full.isLineEllipsized(full.lineCount - 1))
        return if (fits) max else with(steps) { getFontSize(constraints, text) }
    }
}

/** 15/20 semibold title used in widget headers. */
@Composable
internal fun WidgetTitle(text: String, modifier: Modifier = Modifier, color: Color = DNColor.textPrimary) {
    Text(text, modifier, style = widgetType(15f, 600, lineHeight = 20f), color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Small status pill inside widgets ("Next due tomorrow", "Safe to swim"). */
@Composable
internal fun WidgetChip(
    text: String,
    foreground: Color,
    background: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dot: Color? = null,
    height: Dp = 24.dp,
) {
    Row(
        modifier.height(height).clip(CircleShape).background(background).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        if (icon != null) Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(13.dp))
        Text(
            text, style = widgetType(if (height > 24.dp) 13f else 12f, 600, lineHeight = 16f), color = foreground,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 6 dp meter: neutral track, coloured fill. The caption always states the level too. */
@Composable
internal fun WidgetMeter(fraction: Double, color: Color, modifier: Modifier = Modifier, track: Color = DNColor.neutral200) {
    val f by animateFloatAsState(fraction.toFloat().coerceIn(0f, 1f), tween(600, easing = FastOutSlowInEasing), label = "meter")
    val c by animateColorAsState(color, tween(400), label = "meter-colour")
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        val w = if (f > 0f) max(size.height, size.width * f) else 0f
        if (w > 0f) drawRoundRect(c, size = Size(w, size.height), cornerRadius = r)
    }
}

/** A segmented stages bar where the current stage is half filled. */
@Composable
internal fun WidgetStagesBar(total: Int, current: Int, ink: Color, track: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max(total, 1)) { i ->
            Canvas(Modifier.weight(1f).height(6.dp)) {
                val r = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(if (i < current) ink else track, cornerRadius = r)
                if (i == current) drawRoundRect(ink, size = Size(size.width / 2, size.height), cornerRadius = r)
            }
        }
    }
}

/** "Plate   M 48213" — a label and a tabular value on one line. */
@Composable
internal fun WidgetFactRow(label: String, value: String, onDark: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label, style = widgetType(13f, 400, lineHeight = 18f),
            color = if (onDark) DNColor.widgetOnInverseSecondary else DNColor.textSecondary, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Text(
            value, style = widgetType(15f, 600, lineHeight = 20f, tabular = true),
            color = if (onDark) DNColor.widgetOnInverse else DNColor.textPrimary, maxLines = 1,
        )
    }
}

// MARK: - Glyphs (the city cues)

/** The parking sign: white P on Parkin blue with a white inset rule. */
@Composable
internal fun WidgetParkingSign(size: Dp = 34.dp, dimmed: Boolean = false) {
    val shape = RoundedCornerShape(size * 0.26f)
    Box(
        Modifier.size(size).clip(shape).background(if (dimmed) DNColor.neutral200 else DNColor.signParking)
            .padding(1.dp)
            .let { if (dimmed) it else it.border(2.dp, DNColor.white, RoundedCornerShape(size * 0.26f - 1.dp)) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "P", style = widgetType(size.value * 0.62f, 800, lineHeight = size.value * 0.7f),
            color = if (dimmed) DNColor.neutral500 else DNColor.white,
        )
    }
}

/** A round glyph: a Material symbol on a tinted disc. */
@Composable
internal fun WidgetGlyph(icon: ImageVector, ink: Color, fill: Brush, size: Dp = 34.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
internal fun WidgetGlyph(icon: ImageVector, ink: Color, fill: Color, size: Dp = 34.dp) =
    WidgetGlyph(icon, ink, androidx.compose.ui.graphics.SolidColor(fill), size)

/** The Salik gantry (the favorite tile's glyph): a beam on two posts with the reader in the middle. */
@Composable
internal fun WidgetGantryGlyph(ink: Color, fill: Color, size: Dp = 34.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * 0.5f)) {
            val w = this.size.width; val h = this.size.height
            val r = CornerRadius(w * 0.06f, w * 0.06f)
            drawRoundRect(ink, Offset(w * 0.04f, h * 0.10f), Size(w * 0.92f, h * 0.17f), r)
            drawRoundRect(ink, Offset(w * 0.12f, h * 0.20f), Size(w * 0.15f, h * 0.74f), r)
            drawRoundRect(ink, Offset(w * 0.73f, h * 0.20f), Size(w * 0.15f, h * 0.74f), r)
            drawRoundRect(ink, Offset(w * 0.40f, h * 0.30f), Size(w * 0.20f, h * 0.22f), r)
        }
    }
}

/** The bundle mark: three overlapping accent circles on a subtle disc (or a small capsule badge). */
@Composable
internal fun WidgetBundleMark(size: Dp = 34.dp, badge: Boolean = false) {
    val dots: @Composable () -> Unit = {
        Canvas(Modifier.size(15.2.dp, 7.2.dp)) {
            val r = this.size.height / 2
            repeat(3) { i -> drawCircle(DNColor.actionAccent, r, Offset(r + i * 4.dp.toPx(), r)) }
        }
    }
    if (badge) {
        Box(Modifier.height(22.dp).clip(CircleShape).background(DNColor.actionAccentSubtle).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) { dots() }
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(DNColor.actionAccentSubtle), contentAlignment = Alignment.Center) { dots() }
    }
}

/** The nol card glyph: a small silver card with a red dot. */
@Composable
internal fun WidgetNolCard() {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier.size(44.dp, 30.dp)
            .background(Color.Transparent)
            .clip(shape)
            .background(Brush.linearGradient(listOf(DNColor.neutral50, DNColor.neutral300)))
            .border(0.5.dp, DNColor.plateRule, shape)
            .padding(start = 5.dp, end = 5.dp, bottom = 3.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("nol", style = widgetType(11f, 800, lineHeight = 12f, tracking = -0.2f), color = DNColor.textPrimary)
            Spacer(Modifier.weight(1f))
            Box(Modifier.padding(bottom = 1.dp).size(7.dp).clip(CircleShape).background(DNColor.signMetroRed))
        }
    }
}

/** The green Live dot. When an update arrives, one ring expands 1 → 2.4× and fades (900 ms). Never loops. */
@Composable
internal fun WidgetLivePulse(trigger: Int, animate: Boolean) {
    val t = remember { Animatable(1f) }
    LaunchedEffect(trigger, animate) {
        if (!animate) return@LaunchedEffect
        t.snapTo(0f)
        t.animateTo(1f, tween(900))
    }
    Canvas(Modifier.size(7.dp)) {
        val r = size.minDimension / 2
        if (t.value < 1f) {
            drawCircle(DNColor.signLive.copy(alpha = 0.9f * (1f - t.value)), radius = r * (1f + 1.4f * t.value), style = Stroke(1.5.dp.toPx()))
        }
        drawCircle(DNColor.signLive, radius = r)
    }
}

// MARK: - Calm states

/** "Not connected" / "Not parked" / nothing-live layout: glyph · title + line · action (optional). */
@Composable
internal fun WidgetCalmState(
    title: String,
    message: String,
    onDark: Boolean = false,
    quick: (@Composable () -> Unit)? = null,
    glyph: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)) {
        Box(Modifier.height(44.dp), contentAlignment = Alignment.CenterStart) { glyph() }
        Spacer(Modifier.heightIn(min = 6.dp).weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title, style = widgetType(15f, 600, lineHeight = 20f), color = if (onDark) DNColor.widgetOnInverse else DNColor.textPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            WidgetCaption(message, color = if (onDark) DNColor.widgetOnInverseSecondary else DNColor.textSecondary, lines = 3)
        }
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        quick?.invoke()
    }
}

// MARK: - Formatting

internal object WidgetFormat {
    /** "12:12" style without AM/PM, as the prayer widget shows it ("3:35"). */
    fun clock(d: LocalDateTime): String = clock(d.hour, d.minute)

    fun clock(hour: Int, minute: Int): String {
        val h = if (hour % 12 == 0) 12 else hour % 12
        return "%d:%02d".format(Locale.US, h, minute)
    }

    /** "2 hours 31 minutes" for TalkBack. */
    fun spokenDuration(minutes: Int): String {
        val h = minutes / 60; val m = minutes % 60
        val parts = mutableListOf<String>()
        if (h > 0) parts += if (h == 1) "1 hour" else "$h hours"
        if (m > 0 || h == 0) parts += if (m == 1) "1 minute" else "$m minutes"
        return parts.joinToString(" ")
    }

    /**
     * "20 Sep", like iOS en_GB. `DemoDate.short` uses Locale.UK, which Android's CLDR abbreviates to
     * "Sept" (shared request in NOTES.md); this keeps the widgets on the iOS copy until it's fixed.
     */
    fun shortDate(d: LocalDateTime): String = iosMonth(DemoDate.short(d))

    /** "Sept" → "Sep" in a formatted date. */
    fun iosMonth(s: String): String = s.replace("Sept", "Sep")

    /** "September" for a date. */
    fun month(d: LocalDateTime): String = d.format(DateTimeFormatter.ofPattern("MMMM", Locale.UK))

    /** "Aug" from "September". */
    fun previousMonthShort(monthName: String): String {
        val months = java.time.Month.entries
        val i = months.indexOfFirst { it.getDisplayName(JavaTextStyle.FULL, Locale.UK) == monthName }
        if (i < 0) return "last month"
        return months[(i + 11) % 12].getDisplayName(JavaTextStyle.SHORT, Locale.UK).take(3)
    }

    /** "Electricity & water bill" → "electricity and water bill". */
    fun lowerFirst(s: String): String {
        val t = s.replace(" & ", " and ")
        return if (t.isEmpty()) t else t.first().lowercase() + t.drop(1)
    }

    fun capitalizedFirst(s: String): String = if (s.isEmpty()) s else s.first().uppercase() + s.drop(1)
}
