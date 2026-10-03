package com.salehtl.dubainownext.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.JourneyProgress
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.icon
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle

enum class RowTrailing { Chevron, Favorite, None }

/**
 * One row for any catalogue record, used by Services and Search.
 * Standalone: hue disc · name · owner. Bundle: discs · "All at once" · covers. Journey: route · "Start to finish" · scale line.
 */
@Composable
fun CatalogueRow(
    service: CatalogueService,
    modifier: Modifier = Modifier,
    trailing: RowTrailing = RowTrailing.Chevron,
    showsIcon: Boolean = true,
    context: String? = null,
    contextTone: ChipTone = ChipTone.Neutral,
    highlight: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 72.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(start = 16.dp, end = if (trailing == RowTrailing.Favorite) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showsIcon) {
            when (service.type) {
                ServiceType.Standalone -> HueIcon(service.icon, service.hue, size = 40.dp)
                ServiceType.Bundle -> BundleDiscs(service, size = 40.dp)
                ServiceType.Journey -> HueIcon("point.topleft.down.to.point.bottomright.curvepath", service.hue, size = 40.dp)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(highlighted(service.name, highlight), style = DNTextStyle.RowTitle.style, color = DNColor.listRowTitle)
            if (service.type != ServiceType.Standalone) {
                ServiceTypeBadge(service.type)
                val line = if (service.type == ServiceType.Bundle) service.covers?.let { "Covers $it" } else service.scaleLine
                line?.let { Text(it, style = DNTextStyle.Footnote.style, color = DNColor.listRowSupporting, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            } else {
                Text(meta(service, context, contextTone), style = DNTextStyle.Footnote.style, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        when (trailing) {
            RowTrailing.Chevron -> Icon(DNIcons.forSymbol("chevron.forward"), contentDescription = null, tint = DNColor.listRowChevron, modifier = Modifier.size(20.dp))
            RowTrailing.Favorite -> FavoriteStar(service)
            RowTrailing.None -> Unit
        }
    }
}

private fun highlighted(name: String, h: String?): AnnotatedString {
    if (h.isNullOrBlank()) return AnnotatedString(name)
    val i = name.lowercase().indexOf(h.lowercase())
    if (i < 0) return AnnotatedString(name)
    return buildAnnotatedString {
        append(name.substring(0, i))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name.substring(i, i + h.length)) }
        append(name.substring(i + h.length))
    }
}

private fun meta(s: CatalogueService, context: String?, tone: ChipTone): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = DNColor.listRowSupporting)) { append(s.ownerLabel) }
    s.statusLabel?.let {
        withStyle(SpanStyle(color = DNColor.listRowSupporting)) { append(" · ") }
        withStyle(SpanStyle(color = if (s.status == "maintenance") DNColor.statusWarningForeground else DNColor.statusInfoForeground)) { append(it) }
    }
    context?.let {
        withStyle(SpanStyle(color = DNColor.listRowSupporting)) { append(" · ") }
        val c = when (tone) {
            ChipTone.Warning -> DNColor.statusWarningForeground; ChipTone.Error -> DNColor.statusErrorForeground
            ChipTone.Success -> DNColor.statusSuccessForeground; ChipTone.Info -> DNColor.statusInfoForeground
            ChipTone.Neutral -> DNColor.listRowSupporting
        }
        withStyle(SpanStyle(color = c)) { append(it) }
    }
}

/** The favorite star (48 dp target) with toasts and the 8-of-8 limit. */
@Composable
fun FavoriteStar(service: CatalogueService) {
    val store = LocalStore.current; val toasts = LocalToasts.current; val view = LocalView.current
    val isFav = store.isFavorite(service.id)
    IconButton(onClick = {
        if (store.toggleFavorite(service)) {
            if (!isFav) { DNHaptics.success(view); toasts.show("Added to Favorites", Icons.Rounded.Star, undo = { store.toggleFavorite(service) }) }
            else { DNHaptics.light(view); toasts.show("Removed from Favorites", Icons.Rounded.StarOutline) }
        } else {
            DNHaptics.warning(view); toasts.show("Favorites is full (8 of 8). Remove one first.", Icons.Rounded.Error)
        }
    }, modifier = Modifier.semantics { contentDescription = if (isFav) "Remove ${service.short} from Favorites" else "Add ${service.short} to Favorites" }) {
        Icon(if (isFav) Icons.Rounded.Star else Icons.Rounded.StarOutline, contentDescription = null, tint = if (isFav) DNColor.actionAccent else DNColor.iconSecondary)
    }
}

/** A cluster of a bundle's first three parts (the "All at once" visual). */
@Composable
fun BundleDiscs(service: CatalogueService, size: Dp = 40.dp) {
    val parts = service.resolvedParts.take(3)
    val disc = size * 0.56f
    Box(Modifier.size(size)) {
        parts.forEachIndexed { i, part ->
            val (x, y) = when (i) { 0 -> 0.dp to 0.dp; 1 -> (size - disc) to (size * 0.06f); else -> (size * 0.42f - disc / 2) to (size - disc) }
            Box(Modifier.offset(x, y).size(disc).clip(CircleShape).border(1.5.dp, DNColor.surfacePrimary, CircleShape)) {
                HueIcon(part.icon, part.hue, size = disc)
            }
        }
    }
}

/** A journey as a card: hue wash, "Start to finish", scale line and its stages as a bar. */
@Composable
fun JourneyCard(service: CatalogueService, modifier: Modifier = Modifier, progress: JourneyProgress? = null, width: Dp? = 272.dp, onClick: (() -> Unit)? = null) {
    val stages = service.stages ?: emptyList()
    Column(
        modifier.let { if (width != null) it.width(width) else it.fillMaxWidth() }.heightIn(min = 176.dp)
            .dnElevation(DNRadius.card).clip(RoundedCornerShape(DNRadius.card)).background(service.hue.gradient)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ServiceTypeBadge(ServiceType.Journey, onHue = true)
            Spacer(Modifier.weight(1f))
            Icon(service.icon, contentDescription = null, tint = service.hue.ink, modifier = Modifier.size(20.dp))
        }
        Text(service.name, style = DNTextStyle.Title3.style, color = DNColor.textPrimary)
        Text(progress?.let { "Stage ${it.stageIndex + 1} of ${stages.size} · ${it.headline}" } ?: (service.scaleLine ?: ""),
            style = DNTextStyle.Footnote.style, color = DNColor.textOnHueSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f, fill = false))
        SegmentedProgress(
            total = maxOf(stages.size, 1), done = progress?.stageIndex ?: 0, currentIsActive = progress != null,
            doneColor = service.hue.ink, currentColor = service.hue.ink.copy(alpha = 0.55f), trackColor = DNColor.progressTrackOnHue,
        )
        Text(stages.joinToString(" · ") { it.name }, style = DNTextStyle.Caption1.style, color = DNColor.textOnHueSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A bundle as a card: white, discs, "All at once", the count line. */
@Composable
fun BundleCard(service: CatalogueService, modifier: Modifier = Modifier, count: Int? = null, width: Dp? = 272.dp, onClick: (() -> Unit)? = null) {
    Column(
        modifier.let { if (width != null) it.width(width) else it.fillMaxWidth() }.heightIn(min = 176.dp).dnCard()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ServiceTypeBadge(ServiceType.Bundle)
            Spacer(Modifier.weight(1f))
            BundleDiscs(service, size = 36.dp)
        }
        Text(service.name, style = DNTextStyle.Title3.style, color = DNColor.textPrimary)
        Text(count?.let { service.countLine(it) } ?: service.covers?.let { "Covers $it" } ?: "", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        Text(service.description, style = DNTextStyle.Caption1.style, color = DNColor.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}
