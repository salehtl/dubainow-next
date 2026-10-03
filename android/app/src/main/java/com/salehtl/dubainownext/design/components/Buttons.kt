package com.salehtl.dubainownext.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNTextStyle

enum class DNButtonKind { Primary, Secondary, Tertiary, Destructive, Inverse }
enum class DNButtonSize(val height: Dp, val inset: Dp) { Large(56.dp, 24.dp), Medium(48.dp, 20.dp), Small(36.dp, 16.dp) }

/**
 * Canvas buttons are flat pills (v0.5). Android heights: Large 56, Medium 48, Small 36
 * (Small sits inside a 48 dp target). Ripple instead of the iOS press highlight.
 */
@Composable
fun DNButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: DNButtonKind = DNButtonKind.Primary,
    size: DNButtonSize = DNButtonSize.Large,
    fullWidth: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val (fg, bg) = when {
        !enabled -> DNColor.textDisabled to DNColor.textPrimary.copy(alpha = 0.08f)
        kind == DNButtonKind.Primary -> DNColor.textOnAccent to DNColor.actionAccent
        kind == DNButtonKind.Secondary -> DNColor.textLink to DNColor.actionAccentSubtle
        kind == DNButtonKind.Tertiary -> DNColor.textLink to Color.Transparent
        kind == DNButtonKind.Destructive -> DNColor.actionDestructive to DNColor.surfacePrimary
        else -> DNColor.textOnInverse to DNColor.surfaceInverse
    }
    Box(
        modifier
            .let { if (fullWidth) it.fillMaxWidth() else it }
            .heightIn(min = maxOf(size.height, 48.dp))
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .let { if (fullWidth) it.fillMaxWidth() else it }
                .defaultMinSize(minHeight = size.height)
                .clip(CircleShape)
                .background(bg)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = size.inset),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Text(
                text, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = (if (size == DNButtonSize.Small) DNTextStyle.Subheadline else DNTextStyle.Button).style.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            )
        }
    }
}

/**
 * Android replacement for the iOS glass circle buttons: a tonal surface circle with ripple
 * (Paper inventory · Glass button · Adapted: "Tonal surface in place of glass").
 */
@Composable
fun TonalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = DNColor.textPrimary,
    container: Color = DNColor.surfacePrimary.copy(alpha = 0.92f),
    content: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size)
            .dnElevation(size / 2)
            .clip(CircleShape)
            .background(container)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) content() else Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

/** "See all", "Edit", "Change" – inline text actions in the accent colour, 48 dp tall target. */
@Composable
fun DNTextButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = DNColor.textLink) {
    Box(
        modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = DNTextStyle.Callout.style, color = color)
    }
}

/** Press-scale feedback (0.97) for tiles, widgets and cards — use with your own clickable. */
@Composable
fun rememberPressed(interaction: MutableInteractionSource): Boolean {
    val pressed by interaction.collectIsPressedAsState()
    return pressed
}
