package com.salehtl.dubainownext.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle

/** The two-shadow Canvas elevation (near 0 1 2 · far 0 8 24), as a soft Android shadow. */
fun Modifier.dnElevation(radius: Dp = DNRadius.card, enabled: Boolean = true): Modifier =
    if (!enabled) this else this.shadow(
        elevation = 10.dp, shape = RoundedCornerShape(radius), clip = false,
        ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.10f),
    )

/** A white card (radius 20 by default) with the Canvas elevation. Content sits on solid surfaces. */
fun Modifier.dnCard(radius: Dp = DNRadius.card, fill: Color = DNColor.surfacePrimary, elevated: Boolean = true): Modifier =
    this.dnElevation(radius, elevated).clip(RoundedCornerShape(radius)).background(fill)

/** The grey canvas behind grouped screens. */
fun Modifier.dnCanvasBackground(): Modifier = this.background(DNColor.surfaceSecondary)

/**
 * A white rounded group of rows (radius 24) — the Canvas list group. Put [DNRowDivider]
 * between rows, or use [DNListGroupItems] which separates them for you.
 */
@Composable
fun DNListGroup(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (header != null) {
            Text(
                header.uppercase(), style = DNTextStyle.Overline.style, color = DNColor.textGroupedHeader,
                modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() },
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary),
            content = content,
        )
        if (footer != null) {
            Text(footer, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

/** A list group whose rows are separated by inset dividers automatically. */
@Composable
fun <T> DNListGroupItems(
    items: List<T>,
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    row: @Composable (T) -> Unit,
) {
    DNListGroup(modifier, header, footer) {
        items.forEachIndexed { i, item ->
            row(item)
            if (i < items.lastIndex) DNRowDivider()
        }
    }
}

/** Inset separator between rows in a list group. */
@Composable
fun DNRowDivider(inset: Dp = 16.dp) {
    HorizontalDivider(Modifier.padding(start = inset), thickness = 0.5.dp, color = DNColor.listRowSeparator)
}

/**
 * A standard list row: optional leading content, title, optional supporting line,
 * optional trailing value, chevron by default. Minimum height 56 dp (two-line 72) on Android.
 */
@Composable
fun DNListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    showsChevron: Boolean = false, // Paper LOA-0: Android navigation rows have no chevron
    titleColor: Color = DNColor.listRowTitle,
    onClick: (() -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) 56.dp else 72.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke(this)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = DNTextStyle.RowTitle.style, color = titleColor)
            if (subtitle != null) Text(subtitle, style = DNTextStyle.Footnote.style, color = DNColor.listRowSupporting)
        }
        if (value != null) Text(value, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
        trailing?.invoke(this)
        if (showsChevron) {
            Icon(DNIcons.forSymbol("chevron.forward"), contentDescription = null, tint = DNColor.listRowChevron, modifier = Modifier.size(20.dp))
        }
    }
}

/** A plain icon in a 28 dp slot for list rows (settings-style rows). */
@Composable
fun DNRowIcon(icon: ImageVector, tint: Color = DNColor.listRowIcon) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun DNRowIcon(symbol: String, tint: Color = DNColor.listRowIcon) = DNRowIcon(DNIcons.forSymbol(symbol), tint)

/** Horizontal spacer helper. */
@Composable
fun RowScope.Gap(width: Dp) = Spacer(Modifier.width(width))
