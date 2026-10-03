package com.salehtl.dubainownext.design

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.salehtl.dubainownext.R

/**
 * Inter on Android (the design system's Android face; numbers use Inter with tabular figures).
 * One variable font file; each weight is a variation instance.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun interWeight(w: Int) = Font(
    R.font.inter_variable,
    weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

val Inter = FontFamily(
    interWeight(400), interWeight(500), interWeight(600), interWeight(700), interWeight(800),
)

/**
 * Text styles from the Paper type scale (same roles as iOS). Sizes are Paper sizes in sp,
 * so they follow the user's font size setting.
 */
enum class DNTextStyle(
    val size: Float,
    val weight: Int,
    val leading: Float,
    val trackingPx: Float,
    val tabular: Boolean = false,
) {
    LargeTitle(34f, 700, 40f, -0.8f),
    Title1(28f, 700, 34f, -0.55f),
    Title2(22f, 600, 28f, -0.35f),
    /** Section headings on Home and lists ("Needs you", "Favorites") – 22/28 bold. */
    Section(22f, 700, 28f, -0.35f),
    Title3(20f, 600, 26f, -0.25f),
    Headline(17f, 600, 24f, -0.15f),
    Button(17f, 600, 24f, -0.15f),
    Body(17f, 400, 24f, -0.15f),
    Callout(16f, 400, 22f, -0.1f),
    /** List row title – 16/22 medium. */
    RowTitle(16f, 500, 22f, -0.1f),
    Subheadline(15f, 400, 20f, 0f),
    Footnote(13f, 400, 18f, 0f),
    /** Overline ("FROM YOUR ACCOUNTS") – 13 semibold, tracked. */
    Overline(13f, 600, 18f, 0.8f),
    Caption1(12f, 500, 16f, 0f),
    Caption2(11f, 500, 16f, 0f),
    NumericUnit(17f, 600, 22f, 0f, tabular = true),
    NumericMedium(28f, 700, 32f, -0.3f, tabular = true),
    NumericLarge(34f, 700, 40f, -0.5f, tabular = true),
    NumericHero(44f, 700, 46f, -0.8f, tabular = true);

    val style: TextStyle
        get() = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight(weight),
            fontSize = size.sp,
            lineHeight = leading.sp,
            letterSpacing = trackingEm(trackingPx, size),
            fontFeatureSettings = if (tabular) "tnum" else null,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )
}

private fun trackingEm(px: Float, size: Float): TextUnit = (px / size).em

/** Material 3 typography mapped onto the Paper roles, so stock M3 components use Inter too. */
val DNTypography = Typography(
    displaySmall = DNTextStyle.LargeTitle.style,
    headlineLarge = DNTextStyle.LargeTitle.style,
    headlineMedium = DNTextStyle.Title1.style,
    headlineSmall = DNTextStyle.Title2.style,
    titleLarge = DNTextStyle.Title2.style,
    titleMedium = DNTextStyle.Headline.style,
    titleSmall = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight(600)),
    bodyLarge = DNTextStyle.Body.style,
    bodyMedium = DNTextStyle.Callout.style,
    bodySmall = DNTextStyle.Footnote.style,
    labelLarge = DNTextStyle.Button.style,
    labelMedium = DNTextStyle.Caption1.style,
    labelSmall = DNTextStyle.Caption2.style,
)

/** Convenience: the current style with a DN role applied. */
@Composable
fun dnStyle(style: DNTextStyle): TextStyle = LocalTextStyle.current.merge(style.style)
