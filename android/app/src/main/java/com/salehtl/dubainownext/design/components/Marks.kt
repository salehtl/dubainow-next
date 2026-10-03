package com.salehtl.dubainownext.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salehtl.dubainownext.R
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSize
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.Inter
import java.math.BigDecimal

// MARK: Hue icon (disc) and hue tile (squircle)

/** A gradient disc with a glyph in the hue's ink. Sizes: 56 feature, 44 topic, 40 rows, 32 badges. */
@Composable
fun HueIcon(icon: ImageVector, hue: Hue, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(modifier.size(size).clip(CircleShape).background(hue.gradient), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = hue.ink, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun HueIcon(symbol: String, hue: Hue, modifier: Modifier = Modifier, size: Dp = 40.dp) =
    HueIcon(DNIcons.forSymbol(symbol), hue, modifier, size)

/** The favorite tile squircle (r20) with gradient, top highlight and shadow. */
@Composable
fun HueTile(icon: ImageVector, hue: Hue, modifier: Modifier = Modifier, size: Dp = DNSize.favoriteTile) {
    Box(
        modifier
            .size(size)
            .dnElevation(DNRadius.tile)
            .clip(RoundedCornerShape(DNRadius.tile))
            .background(hue.gradient)
            .border(1.dp, Brush.verticalGradient(listOf(DNColor.tileHighlight, Color.Transparent)), RoundedCornerShape(DNRadius.tile)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = hue.ink, modifier = Modifier.size(size * 0.42f))
    }
}

@Composable
fun HueTile(symbol: String, hue: Hue, modifier: Modifier = Modifier, size: Dp = DNSize.favoriteTile) =
    HueTile(DNIcons.forSymbol(symbol), hue, modifier, size)

// MARK: Section header

/** "Needs you", "Favorites", "Your widgets": 22/28 bold with an optional trailing accessory. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accessory: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
        }
        accessory?.invoke(this)
    }
}

// MARK: Status chip

enum class ChipTone { Warning, Info, Success, Error, Neutral }

/** A small status pill with a dot: "Due tomorrow", "In review", "Approved". */
@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier, tone: ChipTone = ChipTone.Neutral, onWhite: Boolean = false) {
    val fg = when (tone) {
        ChipTone.Warning -> DNColor.statusWarningForeground; ChipTone.Info -> DNColor.statusInfoForeground
        ChipTone.Success -> DNColor.statusSuccessForeground; ChipTone.Error -> DNColor.statusErrorForeground
        ChipTone.Neutral -> DNColor.textSecondary
    }
    val surface = when (tone) {
        ChipTone.Warning -> DNColor.statusWarningSurface; ChipTone.Info -> DNColor.statusInfoSurface
        ChipTone.Success -> DNColor.statusSuccessSurface; ChipTone.Error -> DNColor.statusErrorSurface
        ChipTone.Neutral -> DNColor.surfaceSecondary
    }
    Row(
        modifier.heightIn(min = 26.dp).clip(CircleShape).background(if (onWhite) surface else Color.White).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(fg))
        Text(text, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = fg, maxLines = 1)
    }
}

// MARK: Number plate

/** Dubai number plate. Always laid out left to right. */
@Composable
fun NumberPlate(code: String, number: String, modifier: Modifier = Modifier, emirate: String = "DUBAI", small: Boolean = false) =
    FixedFontScale { NumberPlateBody(code, number, modifier, emirate, small) }

@Composable
private fun NumberPlateBody(code: String, number: String, modifier: Modifier, emirate: String, small: Boolean) {
    Row(
        modifier
            .height(if (small) 26.dp else 34.dp)
            .clip(RoundedCornerShape(if (small) 4.dp else 5.dp))
            .background(DNColor.plateSurface)
            .border(1.5.dp, DNColor.plateEdge, RoundedCornerShape(if (small) 4.dp else 5.dp))
            .padding(horizontal = if (small) 7.dp else 9.dp)
            .clearAndSetSemantics { contentDescription = "Plate ${emirate.lowercase().replaceFirstChar { it.uppercase() }} $code $number" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (small) 6.dp else 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emirate, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = if (small) 5.sp else 6.5.sp, color = DNColor.plateInk, lineHeight = 7.sp)
            Text(code, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = if (small) 11.sp else 15.sp, color = DNColor.plateInk, lineHeight = 16.sp)
        }
        Box(Modifier.width(1.dp).height(if (small) 18.dp else 24.dp).background(DNColor.plateRule))
        Text(number, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = if (small) 15.sp else 21.sp, color = DNColor.plateInk)
    }
}

// MARK: Money

/** "AED 412.60" with a small unit and a large figure (Inter, tabular figures). */
@Composable
fun MoneyText(
    amount: BigDecimal,
    modifier: Modifier = Modifier,
    size: DNTextStyle = DNTextStyle.NumericLarge,
    unit: DNTextStyle = DNTextStyle.NumericUnit,
    color: Color = DNColor.textPrimary,
) {
    Row(modifier.clearAndSetSemantics { contentDescription = Money.spoken(amount) }, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("AED", style = unit.style, color = color, modifier = Modifier.padding(bottom = 4.dp))
        Text(Money.figure(amount), style = size.style, color = color)
    }
}

// MARK: Progress

/** Segmented steps bar (e.g. 3 of 5): done segments solid, the current one in accent. */
@Composable
fun SegmentedProgress(
    total: Int,
    done: Int,
    modifier: Modifier = Modifier,
    currentIsActive: Boolean = true,
    doneColor: Color = DNColor.statusSuccessForeground,
    currentColor: Color = DNColor.actionAccent,
    trackColor: Color = DNColor.neutral200,
    height: Dp = 5.dp,
) {
    Row(modifier.fillMaxWidth().semantics { contentDescription = "Step ${minOf(done + 1, total)} of $total" }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(maxOf(total, 1)) { i ->
            val c = if (i < done) doneColor else if (i == done && currentIsActive) currentColor else trackColor
            Box(Modifier.weight(1f).height(height).clip(CircleShape).background(c))
        }
    }
}

// MARK: Service type marks

/** Bundle mark: three stacked circles ("All at once"). */
@Composable
fun StackedCirclesMark(modifier: Modifier = Modifier, color: Color = DNColor.actionAccent, size: Dp = 20.dp) {
    Row(modifier.height(size), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(-(size * 0.32f))) {
        repeat(3) { i ->
            Box(Modifier.size(size * 0.62f).clip(CircleShape).background(Color.White).padding(size * 0.08f).clip(CircleShape).background(color.copy(alpha = 1f - i * 0.25f)))
        }
    }
}

/** Journey mark: a route between two points ("Start to finish"). */
@Composable
fun RouteMark(modifier: Modifier = Modifier, color: Color = DNColor.textPrimary, size: Dp = 20.dp) {
    Icon(DNIcons.forSymbol("point.topleft.down.to.point.bottomright.curvepath"), contentDescription = null, tint = color, modifier = modifier.size(size))
}

/** "All at once" / "Start to finish" label for bundles and journeys. Standalone services have no badge. */
@Composable
fun ServiceTypeBadge(type: ServiceType, modifier: Modifier = Modifier, onHue: Boolean = false) {
    when (type) {
        ServiceType.Bundle -> Row(
            modifier.heightIn(min = 24.dp).clip(CircleShape).background(if (onHue) DNColor.chipOnHue else DNColor.actionAccentSubtle).padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StackedCirclesMark(size = 16.dp)
            Text("All at once", style = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink)
        }
        ServiceType.Journey -> Row(
            modifier.heightIn(min = 24.dp).clip(CircleShape).background(if (onHue) DNColor.chipOnHue else DNColor.surfaceSecondary).padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            RouteMark(size = 14.dp)
            Text("Start to finish", style = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
        }
        ServiceType.Standalone -> Unit
    }
}

// MARK: Happiness faces

/** The three Happiness Meter faces (drawn, so they match iOS exactly). */
enum class HappinessFace(val label: String) {
    Happy("Happy"), Neutral("Neutral"), Unhappy("Unhappy");

    val ink: Color get() = when (this) { Happy -> DNColor.hueMintInk; Neutral -> DNColor.hueSandInk; Unhappy -> DNColor.hueRoseInk }
    val fill: Color get() = when (this) { Happy -> DNColor.hueMintStart; Neutral -> DNColor.hueSandStart; Unhappy -> DNColor.hueRoseStart }
}

@Composable
fun FaceGlyph(face: HappinessFace, modifier: Modifier = Modifier, size: Dp = 32.dp, color: Color? = null) {
    Canvas(modifier.size(size)) {
        val w = this.size.width; val ink = color ?: face.ink; val line = w * 0.075f
        drawCircle(ink, radius = (w - line) / 2, style = Stroke(line))
        val eyeR = w * 0.06f
        drawCircle(ink, eyeR, Offset(w * 0.36f, w * 0.38f)); drawCircle(ink, eyeR, Offset(w * 0.64f, w * 0.38f))
        val y = w * 0.64f
        val mouth = Path().apply {
            when (face) {
                HappinessFace.Happy -> { moveTo(w * 0.32f, y - w * 0.03f); quadraticTo(w * 0.5f, y + w * 0.14f, w * 0.68f, y - w * 0.03f) }
                HappinessFace.Neutral -> { moveTo(w * 0.34f, y); lineTo(w * 0.66f, y) }
                HappinessFace.Unhappy -> { moveTo(w * 0.32f, y + w * 0.07f); quadraticTo(w * 0.5f, y - w * 0.09f, w * 0.68f, y + w * 0.07f) }
            }
        }
        drawPath(mouth, ink, style = Stroke(line, cap = StrokeCap.Round))
    }
}

/** Three faces in a row. One tap rates; the chosen face pops and the others dim. */
@Composable
fun HappinessFacesPicker(selection: HappinessFace?, onSelect: (HappinessFace) -> Unit, modifier: Modifier = Modifier, size: Dp = 56.dp, showsLabels: Boolean = true) {
    val view = LocalView.current
    Row(modifier.fillMaxWidth().semantics { contentDescription = "How do you feel about it?" }) {
        HappinessFace.entries.forEach { face ->
            val scale by animateFloatAsState(if (selection == face) 1.12f else 1f, DNMotion.bouncy(), label = "face")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.RadioButton) { DNHaptics.selection(view); onSelect(face) }
                    .clearAndSetSemantics { selected = selection == face; contentDescription = face.label; role = Role.RadioButton }
                    .alpha(if (selection == null || selection == face) 1f else 0.45f)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(size).scale(scale).clip(CircleShape).background(face.fill)
                        .then(if (selection == face) Modifier.border(2.5.dp, face.ink, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) { FaceGlyph(face, size = size * 0.55f) }
                if (showsLabels) Text(face.label, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
            }
        }
    }
}

// MARK: Logo and avatar

/** The DubaiNow mark (vector). The assistant's face. */
@Composable
fun DNLogo(modifier: Modifier = Modifier, height: Dp = 28.dp) {
    Image(painterResource(R.drawable.dn_mark), contentDescription = "DubaiNow", modifier = modifier.height(height).width(height * 1.05f))
}

/** Monogram avatar on a hue gradient (personas don't have photos in the demo). */
@Composable
fun Avatar(initials: String, hue: Hue, modifier: Modifier = Modifier, size: Dp = 36.dp) = FixedFontScale {
    Box(
        modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(hue.end, Hue.Lilac.end))),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.42f).sp, color = hue.ink,
            modifier = Modifier.clearAndSetSemantics { })
    }
}

/** Draws [content] at font scale 1 (things with a fixed drawn size, like plates and monograms). */
@Composable
fun FixedFontScale(content: @Composable () -> Unit) {
    val d = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, 1f), content = content,
    )
}

/** Offset helper for overlapping marks. */
fun Modifier.nudge(x: Dp = 0.dp, y: Dp = 0.dp) = this.offset(x, y)
