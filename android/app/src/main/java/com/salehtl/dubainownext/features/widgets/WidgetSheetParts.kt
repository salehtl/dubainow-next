package com.salehtl.dubainownext.features.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.TonalIconButton

// Pieces shared by the widget sheets (Add widgets, Extend parking, Top up Salik).
// Android sheets (Paper Android kit · M5 Overlays): M3 modal bottom sheet with a drag handle,
// the title on the start side and a close button on the end side.

/** Sheet title row: title at the start, a tonal close button at the end. */
@Composable
internal fun WidgetSheetHeader(title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title, style = DNTextStyle.Title3.style, color = DNColor.textPrimary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        TonalIconButton(Icons.Rounded.Close, contentDescription = "Close", onClick = onClose, container = DNColor.surfacePrimary)
    }
}

/** "ADD TIME", "AMOUNT": the overline above a group of choices. */
@Composable
internal fun WidgetOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), style = DNTextStyle.Overline.style, color = DNColor.textGroupedHeader,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * A choice in a single-select row (add time, amount), drawn as the Android kit draws it in the Extend
 * parking sheet: outlined when off, tonal accent when on, a 48 dp pill with a state layer. Announced
 * as a radio button so TalkBack says which one is selected.
 */
@Composable
internal fun WidgetChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
) {
    Box(
        modifier
            .heightIn(min = height)
            .clip(CircleShape)
            .background(if (selected) DNColor.actionAccentSubtle else DNColor.surfacePrimary)
            .border(1.dp, if (selected) Color.Transparent else DNColor.borderControl, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = DNTextStyle.Subheadline.style.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) DNColor.textLink else DNColor.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/**
 * The app typography mixes sp (0.sp) and em letter spacing, and M3 text fields lerp bodyLarge → bodySmall
 * for their floating label, which throws ("Cannot perform operation for Em and Sp"). This gives the
 * content the same typography with every letter spacing in em. (Shared request in NOTES.md.)
 */
@Composable
internal fun WidgetFieldSafeTypography(content: @Composable () -> Unit) {
    val t = MaterialTheme.typography
    fun TextStyle.inEm(): TextStyle =
        if (letterSpacing.isSp && fontSize.isSp && fontSize.value > 0f) copy(letterSpacing = (letterSpacing.value / fontSize.value).em) else this
    val safe = remember(t) {
        t.copy(
            displayLarge = t.displayLarge.inEm(), displayMedium = t.displayMedium.inEm(), displaySmall = t.displaySmall.inEm(),
            headlineLarge = t.headlineLarge.inEm(), headlineMedium = t.headlineMedium.inEm(), headlineSmall = t.headlineSmall.inEm(),
            titleLarge = t.titleLarge.inEm(), titleMedium = t.titleMedium.inEm(), titleSmall = t.titleSmall.inEm(),
            bodyLarge = t.bodyLarge.inEm(), bodyMedium = t.bodyMedium.inEm(), bodySmall = t.bodySmall.inEm(),
            labelLarge = t.labelLarge.inEm(), labelMedium = t.labelMedium.inEm(), labelSmall = t.labelSmall.inEm(),
        )
    }
    MaterialTheme(colorScheme = MaterialTheme.colorScheme, shapes = MaterialTheme.shapes, typography = safe, content = content)
}
