package com.salehtl.dubainownext.features.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSize
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.Inter
import com.salehtl.dubainownext.design.cssGradient
import com.salehtl.dubainownext.design.components.DNLogo
import com.salehtl.dubainownext.design.components.RouteMark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Story heroes (Paper C1 · Welcome, "Story hero · …" frames). Each hero is drawn from real Canvas objects on
// Paper's 402 × 392 hero canvas, at Paper's positions and tilts, and the player scales the canvas to fit.
// Heroes are decorative: TalkBack skips them (the story area is one element). Text inside a hero is part of
// the picture, so it is drawn at a fixed size (the player sets fontScale 1 here; the copy below still scales).

/** True when the system "Remove animations" setting is on (or the `welcomeReduceMotion` debug extra). */
internal val LocalWelcomeReduceMotion = compositionLocalOf { false }

/** How much the player scaled the hero canvas down to fit (0.5…1). */
internal val LocalWelcomeHeroScale = compositionLocalOf { 1f }

/** Paper's hero frame is 402 dp wide; every object fits in the top 392 dp of it. */
internal val WelcomeHeroWidth = 402.dp
internal val WelcomeHeroHeight = 392.dp

/** Sara's sample figures used by the tour (rules board: "Its numbers are Sara's sample data"). */
private object WelcomeSample {
    val dewaBill: BigDecimal = BigDecimal("412.60")
    val payAll: BigDecimal = BigDecimal("1012.60")
}

/** One hero on its fixed canvas. `onEvents` is the "See events" link sticker (story 6). */
@Composable
internal fun WelcomeHero(kind: WelcomeHeroKind, onEvents: () -> Unit) {
    val time = rememberWelcomeTime()
    Box(Modifier.size(WelcomeHeroWidth, WelcomeHeroHeight)) {
        when (kind) {
            WelcomeHeroKind.Orbit -> WelcomeOrbitHero(time)
            WelcomeHeroKind.NeedsYou -> WelcomeNeedsYouHero()
            WelcomeHeroKind.Widgets -> WelcomeWidgetsHero(time)
            WelcomeHeroKind.Journey -> WelcomeJourneyHero(time)
            WelcomeHeroKind.Search -> WelcomeSearchHero(time)
            WelcomeHeroKind.Poster -> WelcomePosterHero(time, onEvents)
        }
    }
}

// MARK: - Canvas, placement and motion

/** Seconds since the hero appeared, for the little life while a story plays. Stays 0 with Remove animations. */
@Composable
private fun rememberWelcomeTime(): State<Float> {
    val reduce = LocalWelcomeReduceMotion.current
    val time = remember { mutableFloatStateOf(0f) }
    if (!reduce) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                time.floatValue = (now - start) / 1_000_000_000f
            }
        }
    }
    return time
}

/**
 * Places an object like Paper does (left/top, tilted about its top-left corner) and lets it rise into place with
 * the smooth spring, 40 ms after the one before, settling at its tilt (rules board · Motion).
 * Remove animations: no movement, just a 250 ms fade.
 */
@Composable
private fun BoxScope.WelcomePlaced(
    x: Int,
    y: Int,
    order: Int,
    tilt: Float = 0f,
    rises: Boolean = true,
    content: @Composable () -> Unit,
) {
    val reduce = LocalWelcomeReduceMotion.current
    val settle = remember { Animatable(if (reduce || !rises) 1f else 0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduce) {
            alpha.animateTo(1f, tween(250))
            return@LaunchedEffect
        }
        delay(order * 40L)
        launch { settle.animateTo(1f, DNMotion.smooth()) }
        alpha.animateTo(1f, DNMotion.smooth())
    }
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(x.dp, y.dp)
            .graphicsLayer {
                val p = settle.value
                val s = 0.96f + 0.04f * p
                scaleX = s
                scaleY = s
                rotationZ = tilt * (0.3f + 0.7f * p)
                translationY = (1f - p) * 28.dp.toPx()
                this.alpha = alpha.value.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0f, 0f)
            },
    ) { content() }
}

/** A slow, small float so the hero feels alive while the story plays. Still with Remove animations. */
private fun Modifier.welcomeFloat(time: State<Float>, period: Float, phase: Float, amplitude: Dp = 3.dp): Modifier =
    graphicsLayer { translationY = amplitude.toPx() * sin((time.value / period + phase) * 2f * PI.toFloat()) }

/** Canvas shadow tinted with the story's hue ink (Android draws one soft shadow for the near + far pair). */
private fun Modifier.welcomeShadow(shape: Shape, tint: Color, elevation: Dp, strength: Float = 1f): Modifier =
    shadow(elevation, shape, clip = false, ambientColor = tint.copy(alpha = 0.5f * strength), spotColor = tint.copy(alpha = strength))

/** Inter at a fixed picture size (fontScale is 1 inside heroes). */
private fun welcomeType(size: Float, weight: Int = 400, tracking: Float = 0f, tabular: Boolean = false, leading: Float = size * 1.25f) =
    TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight(weight),
        fontSize = size.sp,
        lineHeight = leading.sp,
        letterSpacing = if (tracking == 0f) 0.sp else (tracking / size).em,
        fontFeatureSettings = if (tabular) "tnum" else null,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

@Composable
private fun WelcomeLabel(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, align: TextAlign? = null) =
    Text(text, modifier = modifier, style = style, color = color, maxLines = 1, softWrap = false, textAlign = align)

/** The soft white glow behind every hero (radial, 85% → 0 at 70%). */
@Composable
private fun BoxScope.WelcomeGlow(x: Int = 41, y: Int = 25) {
    WelcomePlaced(x, y, order = 0, rises = false) {
        Canvas(Modifier.size(320.dp)) {
            drawCircle(
                Brush.radialGradient(
                    listOf(DNColor.white.copy(alpha = 0.85f), DNColor.white.copy(alpha = 0f)),
                    center = center,
                    radius = 158.dp.toPx(),
                ),
                radius = size.minDimension / 2,
            )
        }
    }
}

private fun DrawScope.at(x: Float, y: Float) = Offset(x.dp.toPx(), y.dp.toPx())

private val WelcomeTileHighlight = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.6f), 0.5f to Color.Transparent)

// MARK: - Shared Canvas objects

/** White chip on a hue (brief card status, "3 days left"). */
@Composable
private fun WelcomeChip(text: String, tone: Color = DNColor.textPrimary, dot: Boolean = false) {
    Row(
        Modifier.height(26.dp).clip(CircleShape).background(DNColor.white).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (dot) Box(Modifier.size(7.dp).clip(CircleShape).background(tone))
        WelcomeLabel(text, welcomeType(13f, 600), tone)
    }
}

/** The 32 dp glyph disc at the top of a brief card. */
@Composable
private fun WelcomeGlyphDisc(icon: ImageVector, ink: Color) {
    Box(Modifier.size(32.dp).clip(CircleShape).background(DNColor.posterGlyph), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
    }
}

/** A Canvas tile: hue squircle with a top highlight and a glyph. */
@Composable
private fun WelcomeTile(
    icon: ImageVector,
    size: Int,
    radius: Int,
    fill: Brush,
    ink: Color,
    glyph: Int,
    shadowTint: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier
            .size(size.dp)
            .welcomeShadow(shape, shadowTint, 10.dp, 0.7f)
            .clip(shape)
            .background(fill)
            .border(1.dp, WelcomeTileHighlight, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size((glyph * 1.2f).dp))
    }
}

// MARK: - 1 · Welcome (sky): the orbit of service tiles around the app icon

@Composable
private fun BoxScope.WelcomeOrbitHero(time: State<Float>) {
    val ink = Hue.Sky.ink
    WelcomeGlow()
    WelcomePlaced(0, 0, order = 0, rises = false) { WelcomeOrbitRings(time, Modifier.size(402.dp, 371.dp)) }
    WelcomePlaced(141, 125, order = 1) {
        val shape = RoundedCornerShape(34.dp)
        Box(
            Modifier
                .size(120.dp)
                .welcomeShadow(shape, ink, 18.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(DNColor.white, DNColor.neutral50))),
            contentAlignment = Alignment.Center,
        ) { DNLogo(height = 70.dp) }
    }
    WelcomePlaced(35, 81, order = 2, tilt = -8f) {
        WelcomeTile(Icons.Rounded.LocalParking, 68, 18, SolidColor(DNColor.surfacePrimary), DNColor.actionAccent, 26, ink, Modifier.welcomeFloat(time, 4.2f, 0f))
    }
    WelcomePlaced(200, 10, order = 3, tilt = 6f) {
        WelcomeTile(Icons.Rounded.Toll, 54, 15, Hue.Sand.gradient, Hue.Sand.ink, 20, ink, Modifier.welcomeFloat(time, 3.6f, 0.3f))
    }
    WelcomePlaced(313, 108, order = 4, tilt = 8f) {
        WelcomeTile(Icons.Rounded.Description, 62, 17, Hue.Mint.gradient, Hue.Mint.ink, 23, ink, Modifier.welcomeFloat(time, 4.6f, 0.55f))
    }
    WelcomePlaced(282, 247, order = 5, tilt = -5f) {
        WelcomeTile(Icons.Rounded.DirectionsCar, 68, 18, Hue.Rose.gradient, Hue.Rose.ink, 24, ink, Modifier.welcomeFloat(time, 3.9f, 0.15f))
    }
    WelcomePlaced(116, 297, order = 6, tilt = 9f) {
        WelcomeTile(Icons.Rounded.Bolt, 52, 14, Hue.Peach.gradient, Hue.Peach.ink, 20, ink, Modifier.welcomeFloat(time, 4.4f, 0.7f))
    }
    WelcomePlaced(29, 202, order = 7, tilt = -10f) {
        WelcomeTile(Icons.Rounded.Favorite, 58, 16, Hue.Lilac.gradient, Hue.Lilac.ink, 22, ink, Modifier.welcomeFloat(time, 3.8f, 0.45f))
    }
}

/** Two orbits around the app icon. Their dots drift slowly in opposite directions. */
@Composable
private fun WelcomeOrbitRings(time: State<Float>, modifier: Modifier) {
    val ink = Hue.Sky.ink
    Canvas(modifier) {
        val c = at(201f, 185f)
        val ring = DNColor.white.copy(alpha = 0.9f)
        drawCircle(ring, 150.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
        drawCircle(
            ring, 96.dp.toPx(), c,
            style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx()))),
        )
        val t = time.value
        val outer = (t / 70f % 1f) * 2f * PI.toFloat()
        val inner = -(t / 50f % 1f) * 2f * PI.toFloat()
        fun dot(r: Float, angle: Float, size: Float, color: Color) {
            val p = Offset(c.x + r.dp.toPx() * cos(angle), c.y + r.dp.toPx() * sin(angle))
            drawCircle(color, size.dp.toPx(), p)
        }
        dot(150f, -PI.toFloat() / 2 + outer, 3f, ink)
        dot(150f, outer, 3f, DNColor.white)
        dot(96f, PI.toFloat() + inner, 3.5f, DNColor.white)
        dot(96f, inner, 2.5f, ink)
    }
}

// MARK: - 2 · Needs you (peach): brief cards and the Pay all pill

@Composable
private fun BoxScope.WelcomeNeedsYouHero() {
    val tint = Hue.Peach.ink
    WelcomeGlow()
    WelcomePlaced(12, 42, order = 1, tilt = -8f) { WelcomeResidencyCard(tint) }
    WelcomePlaced(108, 76, order = 2, tilt = 4f) { WelcomeBillCard(tint) }
    WelcomePlaced(30, 322, order = 3, tilt = -3f) { WelcomePayAllPill() }
}

@Composable
private fun WelcomeCardTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        WelcomeLabel(title, welcomeType(17f, 600, -0.15f), DNColor.textPrimary)
        WelcomeLabel(subtitle, welcomeType(15f), DNColor.textSecondary)
    }
}

@Composable
private fun WelcomeResidencyCard(tint: Color) {
    val shape = RoundedCornerShape(DNRadius.brief)
    Column(Modifier.width(DNSize.briefCard).welcomeShadow(shape, tint, 16.dp, 0.8f).clip(shape).background(DNColor.surfacePrimary)) {
        Box(Modifier.fillMaxWidth().height(120.dp).background(Hue.Lilac.start)) {
            Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WelcomeGlyphDisc(Icons.Rounded.Description, Hue.Lilac.ink)
                    Spacer(Modifier.width(8.dp))
                    WelcomeLabel("Residency", welcomeType(13f, 600), DNColor.textPrimary)
                    Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
                    WelcomeChip("3 days left")
                }
                Spacer(Modifier.weight(1f))
                Row {
                    WelcomeLabel("3", welcomeType(34f, 700, -0.5f, tabular = true), DNColor.textPrimary, Modifier.alignByBaseline())
                    Spacer(Modifier.width(6.dp))
                    WelcomeLabel("of 5 steps", welcomeType(17f, 600), DNColor.textPrimary, Modifier.alignByBaseline())
                }
            }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(5.dp).background(DNColor.progressTrackOnHue)) {
                Box(Modifier.width(109.dp).fillMaxHeight().background(DNColor.actionAccent))
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WelcomeCardTitle("Upload passport copy", "Residence renewal · GDRFA")
            Row(
                Modifier.fillMaxWidth().height(44.dp).clip(CircleShape).background(DNColor.actionAccent),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = DNColor.textOnAccent, modifier = Modifier.size(18.dp))
                WelcomeLabel("Continue", welcomeType(17f, 600, -0.15f), DNColor.textOnAccent)
            }
        }
    }
}

@Composable
private fun WelcomeBillCard(tint: Color) {
    val shape = RoundedCornerShape(DNRadius.brief)
    Column(Modifier.width(DNSize.briefCard).welcomeShadow(shape, tint, 20.dp)) {
        Column(Modifier.clip(shape).background(DNColor.surfacePrimary)) {
            Column(Modifier.fillMaxWidth().height(120.dp).background(Hue.Sand.start).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WelcomeGlyphDisc(Icons.Rounded.Bolt, Hue.Sand.ink)
                    Spacer(Modifier.width(8.dp))
                    WelcomeLabel("DEWA", welcomeType(13f, 600), DNColor.textPrimary)
                    Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
                    WelcomeChip("Due tomorrow", tone = DNColor.statusWarningForeground, dot = true)
                }
                Spacer(Modifier.weight(1f))
                Row {
                    WelcomeLabel("AED", welcomeType(17f, 600), DNColor.textPrimary, Modifier.alignByBaseline())
                    Spacer(Modifier.width(5.dp))
                    WelcomeLabel(Money.figure(WelcomeSample.dewaBill), welcomeType(34f, 700, -0.5f, tabular = true), DNColor.textPrimary, Modifier.alignByBaseline())
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WelcomeCardTitle("Electricity & water bill", "Al Barsha 1 · Account 2004 1187")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(DNColor.actionAccent), contentAlignment = Alignment.Center) {
                        WelcomeLabel("Pay now", welcomeType(17f, 600, -0.15f), DNColor.textOnAccent)
                    }
                    Box(Modifier.width(88.dp).height(44.dp).clip(CircleShape).background(DNColor.actionAccentSubtle), contentAlignment = Alignment.Center) {
                        WelcomeLabel("Later", welcomeType(17f, 600, -0.15f), DNColor.textLink)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePayAllPill() {
    Row(
        Modifier
            .height(40.dp)
            .welcomeShadow(CircleShape, DNColor.actionAccent, 14.dp)
            .clip(CircleShape)
            .background(DNColor.actionAccent)
            .padding(start = 14.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Three overlapping dots: the Pay all mark.
        Box(Modifier.width(18.6.dp).height(8.dp)) {
            listOf(0.5f, 0.78f, 1f).forEachIndexed { i, a ->
                Box(Modifier.offset(x = (i * 5.3f).dp).size(8.dp).clip(CircleShape).background(DNColor.white.copy(alpha = a)))
            }
        }
        Spacer(Modifier.width(8.dp))
        WelcomeLabel("Pay all · ${Money.aed(WelcomeSample.payAll)}", welcomeType(16f, 600), DNColor.textOnAccent)
    }
}

// MARK: - 3 · Your widgets (lilac): Salik lifted out of its slot

@Composable
private fun BoxScope.WelcomeWidgetsHero(time: State<Float>) {
    WelcomeGlow()
    WelcomePlaced(208, 28, order = 0, rises = false) {
        // The dashed slot Salik was lifted out of.
        Canvas(Modifier.size(DNSize.widgetUnit)) {
            val r = CornerRadius(DNRadius.widget.toPx())
            drawRoundRect(DNColor.white.copy(alpha = 0.28f), cornerRadius = r)
            val w = 2.dp.toPx()
            drawRoundRect(
                Hue.Lilac.ink.copy(alpha = 0.22f),
                topLeft = Offset(w / 2, w / 2),
                size = Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius(DNRadius.widget.toPx() - w / 2),
                style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        }
    }
    WelcomePlaced(118, 204, order = 3, tilt = -3f) { WelcomePrayerWidget() }
    WelcomePlaced(22, 20, order = 1, tilt = -6f) { WelcomeParkingWidget() }
    WelcomePlaced(200, 4, order = 2, tilt = 8f) { WelcomeSalikWidget(Modifier.welcomeFloat(time, 3.4f, 0.2f, 2.5.dp)) }
}

private val WelcomeWidgetShape = RoundedCornerShape(DNRadius.widget)

@Composable
private fun WelcomeWidgetPlus(fill: Color, ink: Color) {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun WelcomeParkingWidget() {
    Column(
        Modifier
            .size(DNSize.widgetUnit)
            .welcomeShadow(WelcomeWidgetShape, Color.Black, 10.dp, 0.45f)
            .clip(WelcomeWidgetShape)
            .background(DNColor.widgetSurface)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            // Parking sign: blue plate, white inner rule, blue outer ring.
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DNColor.signParking)
                    .padding(1.dp)
                    .border(2.dp, DNColor.white, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) { WelcomeLabel("P", welcomeType(22f, 800), DNColor.white) }
            Spacer(Modifier.weight(1f))
            WelcomeWidgetPlus(DNColor.actionAccentSubtle, DNColor.actionAccent)
        }
        Row(Modifier.padding(top = 6.dp).height(52.dp), verticalAlignment = Alignment.Bottom) {
            WelcomeLabel("42", welcomeType(44f, 700, -0.8f, tabular = true, leading = 50f), DNColor.textPrimary, Modifier.alignByBaseline())
            Spacer(Modifier.width(4.dp))
            WelcomeLabel("min", welcomeType(17f, 600), DNColor.textPrimary, Modifier.alignByBaseline())
        }
        WelcomeLabel("left in Zone 365C", welcomeType(13f), DNColor.textSecondary, Modifier.padding(bottom = 12.dp))
        Box(Modifier.width(143.dp).height(6.dp).clip(CircleShape).background(DNColor.neutral200)) {
            Box(Modifier.width(50.dp).fillMaxHeight().clip(CircleShape).background(DNColor.signParking))
        }
    }
}

@Composable
private fun WelcomeSalikWidget(modifier: Modifier) {
    Column(
        modifier
            .size(DNSize.widgetUnit)
            .welcomeShadow(WelcomeWidgetShape, Hue.Lilac.ink, 24.dp)
            .clip(WelcomeWidgetShape)
            .background(DNColor.widgetSurfaceInverse)
            .drawRoadBehind()
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(DNColor.white.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Toll, contentDescription = null, tint = DNColor.widgetOnInverse, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.weight(1f))
            WelcomeWidgetPlus(DNColor.widgetOnInverseTrack, DNColor.widgetOnInverse)
        }
        Row(Modifier.padding(top = 6.dp).height(52.dp), verticalAlignment = Alignment.Bottom) {
            WelcomeLabel("AED", welcomeType(17f, 600), DNColor.white.copy(alpha = 0.85f), Modifier.alignByBaseline())
            Spacer(Modifier.width(5.dp))
            WelcomeLabel("18", welcomeType(44f, 700, -0.8f, tabular = true, leading = 50f), DNColor.widgetOnInverse, Modifier.alignByBaseline())
        }
        Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(DNColor.signLow))
            WelcomeLabel("Low · about 4 trips", welcomeType(13f, 600), DNColor.signLow)
        }
        Row(Modifier.width(143.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(10) { i ->
                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(if (i < 4) DNColor.signLow else DNColor.widgetOnInverseTrack))
            }
        }
    }
}

/** A faint road running under the Salik balance. */
private fun Modifier.drawRoadBehind(): Modifier = drawBehind {
    drawLine(
        DNColor.white.copy(alpha = 0.07f),
        start = at(118f, 175f),
        end = at(150f, 92f),
        strokeWidth = 10.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16.dp.toPx(), 14.dp.toPx())),
    )
}

@Composable
private fun WelcomePrayerWidget() {
    Column(
        Modifier
            .size(DNSize.widgetUnit)
            .welcomeShadow(WelcomeWidgetShape, Color.Black, 10.dp, 0.45f)
            .clip(WelcomeWidgetShape)
            .background(Brush.verticalGradient(listOf(DNColor.widgetSkyTop, DNColor.widgetSkyBottom)))
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = DNColor.widgetSkyGlyph, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                WelcomeLabel("Dhuhr", welcomeType(15f, 600), DNColor.textPrimary)
                Spacer(Modifier.weight(1f))
                WelcomeLabel("in 2h 31m", welcomeType(13f, 500), DNColor.textOnHueSecondary)
            }
            WelcomeLabel("12:12", welcomeType(40f, 700, -0.8f, tabular = true, leading = 46f), DNColor.textPrimary)
        }
        Spacer(Modifier.weight(1f))
        WelcomeSunArc(Modifier.size(143.dp, 52.dp))
    }
}

/** The sun's path across the sky in the prayer widget (143 × 52). */
@Composable
private fun WelcomeSunArc(modifier: Modifier) {
    Canvas(modifier) {
        fun point(degrees: Float): Offset {
            val a = degrees * PI.toFloat() / 180f
            return at(71.5f + 66f * cos(a), 46f - 40f * sin(a))
        }
        fun arc(from: Float, to: Float): Path = Path().apply {
            val p0 = point(from)
            moveTo(p0.x, p0.y)
            var d = from
            while (d > to) {
                val p = point(d)
                lineTo(p.x, p.y)
                d -= 2f
            }
            val end = point(to)
            lineTo(end.x, end.y)
        }
        drawLine(DNColor.widgetSkyHorizon, at(0f, 46.5f), at(143f, 46.5f), 1.dp.toPx())
        drawPath(
            arc(180f, 0f), DNColor.widgetSkyPath,
            style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))),
        )
        drawPath(arc(180f, 127.8f), DNColor.signSun, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))
        val marker = at(72.2f, 6f)
        drawCircle(DNColor.white, 3.6.dp.toPx(), marker)
        drawCircle(DNColor.widgetSkyGlyph, 3.6.dp.toPx(), marker, style = Stroke(1.8.dp.toPx()))
        listOf(at(123.4f, 21.3f), at(137.5f, 46f)).forEach { drawCircle(DNColor.widgetSkyMarker, 2.6.dp.toPx(), it) }
        val sun = at(31.05f, 14.4f)
        drawCircle(DNColor.widgetSunGlow, 11.dp.toPx(), sun)
        drawCircle(DNColor.widgetSun, 6.5.dp.toPx(), sun)
    }
}

// MARK: - 4 · Start to finish (sand): Build a house, 7 stages

@Composable
private fun BoxScope.WelcomeJourneyHero(time: State<Float>) {
    val ink = Hue.Sand.ink
    WelcomeGlow(41, 60)
    WelcomePlaced(0, 18, order = 0, rises = false) { WelcomeJourneyRoute(time) }
    WelcomePlaced(326, 222, order = 4) {
        val shape = RoundedCornerShape(17.dp)
        Box(
            Modifier
                .size(60.dp)
                .welcomeShadow(shape, ink, 12.dp, 0.8f)
                .clip(shape)
                .background(cssGradient(160.0, listOf(0f to DNColor.white, 1f to Hue.Sand.start)))
                .border(1.dp, WelcomeTileHighlight, shape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Home, contentDescription = null, tint = ink, modifier = Modifier.size(29.dp)) }
    }
    WelcomePlaced(74, 72, order = 1) {
        val shape = RoundedCornerShape(22.dp)
        Column(
            Modifier
                .width(IntrinsicSize.Max)
                .widthIn(min = 244.dp)
                .welcomeShadow(shape, ink, 16.dp, 0.8f)
                .clip(shape)
                .background(DNColor.surfacePrimary)
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(Hue.Sand.start), contentAlignment = Alignment.Center) {
                    RouteMark(color = ink, size = 16.dp)
                }
                Spacer(Modifier.width(8.dp))
                WelcomeLabel("Build a house", welcomeType(15f, 600), DNColor.textPrimary)
                Spacer(Modifier.weight(1f).widthIn(min = 12.dp))
                Box(Modifier.height(24.dp).clip(CircleShape).background(Hue.Sand.start).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    WelcomeLabel("Your turn", welcomeType(12f, 600), ink)
                }
            }
            Row {
                WelcomeLabel("4", welcomeType(34f, 700, tabular = true), DNColor.textPrimary, Modifier.alignByBaseline())
                Spacer(Modifier.width(6.dp))
                WelcomeLabel("of 7 · Design", welcomeType(17f, 600), DNColor.textPrimary, Modifier.alignByBaseline())
            }
        }
    }
}

/** The Build a house route (Paper path, 402 × 371): 3 stages done, "Design" now, 3 ahead. */
@Composable
private fun WelcomeJourneyRoute(time: State<Float>) {
    val ink = Hue.Sand.ink
    val reduce = LocalWelcomeReduceMotion.current
    val drawn = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) {
            delay(120)
            drawn.animateTo(1f, tween(700, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
        }
    }
    val done = listOf(34f to 300f, 86f to 262f, 140f to 286f)
    val ahead = listOf(252f to 262f, 304f to 222f)
    Box(Modifier.size(402.dp, 371.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawPath(welcomeRoutePath(0 until 6), DNColor.white.copy(alpha = 0.7f), style = Stroke(18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(
                welcomeRoutePath(3 until 6), ink,
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1.dp.toPx(), 8.dp.toPx()))),
            )
            val measure = PathMeasure().apply { setPath(welcomeRoutePath(0 until 3), false) }
            val part = Path()
            measure.getSegment(0f, measure.length * drawn.value, part, true)
            drawPath(part, ink, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(
                ink, at(196f, 186f), at(196f, 222f), 2.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1.dp.toPx(), 5.dp.toPx())),
            )
            done.forEach { (x, y) -> drawCircle(ink, 10.dp.toPx(), at(x, y)) }
            ahead.forEach { (x, y) ->
                drawCircle(DNColor.white, 7.dp.toPx(), at(x, y))
                drawCircle(ink, 7.dp.toPx(), at(x, y), style = Stroke(2.dp.toPx()))
            }
        }
        done.forEach { (x, y) ->
            Icon(Icons.Rounded.Check, contentDescription = null, tint = DNColor.white, modifier = Modifier.offset((x - 7).dp, (y - 7).dp).size(14.dp))
        }
        WelcomeCurrentStop(time, ink, Modifier.offset((196 - 26).dp, (240 - 26).dp))
        WelcomeRouteLabel("Plan", 2, 318, done = true)
        WelcomeRouteLabel("Land", 54, 230, done = true)
        WelcomeRouteLabel("Money", 108, 304, done = true)
        WelcomeRouteLabel("Permits", 220, 278, done = false)
        WelcomeRouteLabel("Build", 272, 192, done = false)
        WelcomeRouteLabel("Move in", 324, 272, done = false)
    }
}

@Composable
private fun WelcomeRouteLabel(text: String, x: Int, y: Int, done: Boolean) {
    Box(Modifier.offset(x.dp, y.dp).width(64.dp), contentAlignment = Alignment.TopCenter) {
        WelcomeLabel(text, welcomeType(13f, if (done) 600 else 500), if (done) Hue.Sand.ink else DNColor.textOnWashSecondary)
    }
}

/** The stage you're on: a white halo that breathes gently, ringed in the hue ink. */
@Composable
private fun WelcomeCurrentStop(time: State<Float>, ink: Color, modifier: Modifier) {
    Box(modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(44.dp)
                .graphicsLayer {
                    val breathe = 1f + 0.12f * (0.5f + 0.5f * sin(time.value * 2f * PI.toFloat() / 2.4f))
                    scaleX = breathe
                    scaleY = breathe
                }
                .clip(CircleShape)
                .background(DNColor.white.copy(alpha = 0.55f)),
        )
        Canvas(Modifier.size(24.dp)) {
            drawCircle(DNColor.white)
            drawCircle(ink, style = Stroke(3.5.dp.toPx()))
            drawCircle(ink, 4.5.dp.toPx())
        }
    }
}

/** The route's cubic segments (Paper path), in dp on the 402 × 371 route frame. */
private val WelcomeRouteStart = 34f to 300f
private val WelcomeRouteSegments = listOf(
    floatArrayOf(86f, 262f, 42.7f, 293.7f, 68.3f, 264.3f),
    floatArrayOf(140f, 286f, 103.7f, 259.7f, 121.7f, 289.7f),
    floatArrayOf(196f, 240f, 158.3f, 282.3f, 177.3f, 244.0f),
    floatArrayOf(252f, 262f, 214.7f, 236.0f, 234.0f, 265.0f),
    floatArrayOf(304f, 222f, 270.0f, 259.0f, 286.7f, 226.3f),
    floatArrayOf(356f, 236f, 321.3f, 217.7f, 347.3f, 233.7f),
)

private fun Density.welcomeRoutePath(range: IntRange): Path = Path().apply {
    val (sx, sy) = if (range.first == 0) WelcomeRouteStart else WelcomeRouteSegments[range.first - 1].let { it[0] to it[1] }
    moveTo(sx.dp.toPx(), sy.dp.toPx())
    for (i in range) {
        val s = WelcomeRouteSegments[i]
        cubicTo(s[2].dp.toPx(), s[3].dp.toPx(), s[4].dp.toPx(), s[5].dp.toPx(), s[0].dp.toPx(), s[1].dp.toPx())
    }
}

// MARK: - 5 · Search (mint): the answer card and an alias chip

@Composable
private fun BoxScope.WelcomeSearchHero(time: State<Float>) {
    val tint = Hue.Mint.ink
    WelcomeGlow()
    WelcomePlaced(40, 102, order = 2, tilt = 2f) { WelcomeAnswerCard(tint) }
    WelcomePlaced(28, 30, order = 1, tilt = -2f) { WelcomeSearchField(time, tint) }
    WelcomePlaced(52, 342, order = 3, tilt = -4f) { WelcomeAliasChip(tint) }
}

/** A picture of the search field (heroes are content). */
@Composable
private fun WelcomeSearchField(time: State<Float>, tint: Color) {
    val reduce = LocalWelcomeReduceMotion.current
    Row(
        Modifier
            .size(346.dp, 56.dp)
            .welcomeShadow(CircleShape, tint, 10.dp, 0.6f)
            .clip(CircleShape)
            .background(DNColor.surfacePrimary.copy(alpha = 0.86f))
            .border(1.dp, DNColor.white, CircleShape)
            .padding(start = 18.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        WelcomeLabel("how much do I owe", welcomeType(17f), DNColor.textPrimary)
        Spacer(Modifier.width(1.dp))
        // The text caret, blinking like the real one.
        Box(
            Modifier
                .size(2.dp, 22.dp)
                .graphicsLayer { alpha = if (reduce || (time.value / 0.53f).toInt() % 2 == 0) 1f else 0f }
                .clip(CircleShape)
                .background(DNColor.actionAccent),
        )
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Cancel, contentDescription = null, tint = DNColor.neutral400, modifier = Modifier.size(22.dp))
        }
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun WelcomeAnswerCard(tint: Color) {
    val shape = RoundedCornerShape(DNRadius.widget)
    Column(
        Modifier
            .width(322.dp)
            .welcomeShadow(shape, tint, 18.dp, 0.8f)
            .clip(shape)
            .background(DNColor.surfacePrimary)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            WelcomeLabel("FROM YOUR ACCOUNTS", welcomeType(13f, 600, 0.8f), DNColor.textSecondary)
            Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
            WelcomeLabel("Updated 9:41", welcomeType(13f), DNColor.textSecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            WelcomeLabel("You owe", welcomeType(17f, 600), DNColor.textPrimary)
            Row {
                WelcomeLabel("AED", welcomeType(17f, 600), DNColor.textPrimary, Modifier.alignByBaseline())
                Spacer(Modifier.width(6.dp))
                WelcomeLabel(Money.figure(WelcomeSample.payAll), welcomeType(44f, 700, -0.8f, tabular = true, leading = 50f), DNColor.textPrimary, Modifier.alignByBaseline())
            }
            WelcomeLabel("across 1 bill and 1 fine", welcomeType(15f), DNColor.textSecondary)
        }
        Box(Modifier.fillMaxWidth().height(54.dp).clip(CircleShape).background(DNColor.actionAccent), contentAlignment = Alignment.Center) {
            WelcomeLabel("Pay both · ${Money.aed(WelcomeSample.payAll)}", welcomeType(17f, 600, -0.15f), DNColor.textOnAccent)
        }
    }
}

@Composable
private fun WelcomeAliasChip(tint: Color) {
    Row(
        Modifier
            .height(40.dp)
            .welcomeShadow(CircleShape, tint, 12.dp, 0.8f)
            .clip(CircleShape)
            .background(DNColor.surfacePrimary)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(16.dp))
        WelcomeLabel("amer", welcomeType(15f), DNColor.textSecondary)
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        WelcomeLabel("Renew your residence visa", welcomeType(15f, 600), DNColor.textPrimary)
    }
}

// MARK: - 6 · Announcement (rose): Dubai Fitness Challenge 30×30 poster

@Composable
private fun BoxScope.WelcomePosterHero(time: State<Float>, onLink: () -> Unit) {
    val ink = Hue.Rose.ink
    WelcomeGlow()
    WelcomePlaced(22, 66, order = 0, rises = false) { WelcomeTrack() }
    WelcomePlaced(0, 108, order = 1) {
        Row(Modifier.width(402.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Top) {
            WelcomePosterFigure("MINUTES", ink)
            Box(Modifier.height(112.dp), contentAlignment = Alignment.Center) {
                WelcomeLabel("×", welcomeType(64f, 400, leading = 70f), ink)
            }
            WelcomePosterFigure("DAYS", ink)
        }
    }
    WelcomePlaced(298, 34, order = 2, tilt = 12f) {
        Box(
            Modifier
                .welcomeFloat(time, 3.2f, 0f, 2.5.dp)
                .size(68.dp)
                .welcomeShadow(CircleShape, ink, 12.dp, 0.8f)
                .clip(CircleShape)
                .background(ink),
            contentAlignment = Alignment.Center,
        ) { WelcomeLabel("Free", welcomeType(17f, 700), DNColor.white) }
    }
    WelcomePlaced(124, 306, order = 3, tilt = -3f) {
        // The link sticker: the one tappable thing in a story. It is 44 tall on the canvas and the canvas can be
        // scaled down, so the touch area reaches past the sticker to stay at least 48 dp on screen. The touch box
        // is pulled back by the same amount so the sticker itself stays at Paper's spot (and tilts about it).
        val scale = LocalWelcomeHeroScale.current
        val padY = ((48f / scale - 44f) / 2f).coerceAtLeast(2f).dp
        val padX = 4.dp
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .offset(-padX, -padY)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "See events",
                    onClick = onLink,
                )
                .padding(horizontal = padX, vertical = padY),
        ) {
            Row(
                Modifier
                    .height(44.dp)
                    .welcomeShadow(CircleShape, ink, 12.dp, 0.8f)
                    .clip(CircleShape)
                    .background(DNColor.surfacePrimary)
                    .indication(interaction, ripple())
                    .padding(start = 14.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = DNColor.actionAccent, modifier = Modifier.size(19.dp))
                WelcomeLabel("See events", welcomeType(15f, 600), DNColor.actionAccent)
            }
        }
    }
}

@Composable
private fun WelcomePosterFigure(label: String, ink: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.height(112.dp), contentAlignment = Alignment.Center) {
            WelcomeLabel(
                "30",
                welcomeType(112f, 800, -4.5f, leading = 112f).copy(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
                ink,
            )
        }
        WelcomeLabel(label, welcomeType(13f, 700, 0.8f), ink)
    }
}

/** A running track, three lanes, tilted −7°. */
@Composable
private fun WelcomeTrack() {
    Canvas(Modifier.size(358.dp, 228.dp).graphicsLayer { rotationZ = -7f }) {
        fun lane(w: Float, h: Float, stroke: Stroke, color: Color) {
            val size = Size(w.dp.toPx(), h.dp.toPx())
            val topLeft = Offset((this.size.width - size.width) / 2, (this.size.height - size.height) / 2)
            drawRoundRect(color, topLeft, size, CornerRadius(size.height / 2), style = stroke)
        }
        lane(358f, 228f, Stroke(2.dp.toPx()), DNColor.white.copy(alpha = 0.85f))
        lane(
            318f, 188f,
            Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 7.dp.toPx()))),
            DNColor.white.copy(alpha = 0.85f),
        )
        lane(278f, 148f, Stroke(1.5.dp.toPx()), DNColor.white.copy(alpha = 0.6f))
    }
}
