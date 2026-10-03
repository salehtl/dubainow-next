package com.salehtl.dubainownext.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// MARK: Toasts (M3 snackbar on Android: inverse surface, radius 4)

data class Toast(
    val message: String,
    val icon: ImageVector? = Icons.Rounded.CheckCircle,
    val undoTitle: String? = null,
    val undo: (() -> Unit)? = null,
)

/** A short confirmation above the bottom bar ("Added to Favorites · Undo"). 2.5 s, 5 s with Undo. */
class ToastCenter {
    var current by mutableStateOf<Toast?>(null)
        private set
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun show(message: String, icon: ImageVector? = Icons.Rounded.CheckCircle, undo: (() -> Unit)? = null) =
        show(Toast(message, icon, if (undo != null) "Undo" else null, undo))

    fun show(toast: Toast) {
        job?.cancel()
        current = toast
        job = scope.launch {
            delay(if (toast.undo == null) DNMotion.TOAST_HOLD_MS else DNMotion.TOAST_HOLD_UNDO_MS)
            current = null
        }
    }

    fun dismiss() { job?.cancel(); current = null }
}

/**
 * Place once (inside a Box) at the root, and again inside sheets. With [topInset] the toast sits
 * that far below the status bar instead (for screens whose bottom is a call to action).
 */
@Composable
fun BoxScope.ToastHost(center: ToastCenter, bottomInset: Dp = 104.dp, topInset: Dp? = null) {
    val top = topInset != null
    AnimatedVisibility(
        visible = center.current != null,
        modifier = (if (topInset != null) Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = topInset)
            else Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottomInset)).padding(horizontal = 16.dp),
        enter = slideInVertically(DNMotion.smooth()) { if (top) -it else it } + fadeIn(),
        exit = slideOutVertically(DNMotion.smooth()) { if (top) -it else it } + fadeOut(),
    ) {
        val t = center.current ?: return@AnimatedVisibility
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).dnElevation(4.dp).clip(RoundedCornerShape(DNRadius.snackbar))
                .background(DNColor.surfaceInverse).padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            t.icon?.let { Icon(it, contentDescription = null, tint = DNColor.green300, modifier = Modifier.size(20.dp)) }
            Text(t.message, style = DNTextStyle.Subheadline.style, color = DNColor.textOnInverse, modifier = Modifier.weight(1f))
            if (t.undo != null) {
                DNTextButton(t.undoTitle ?: "Undo", onClick = { t.undo.invoke(); center.dismiss() }, color = DNColor.textLinkOnInverse)
            }
        }
    }
}

// MARK: Coming soon

/** What a demo dead end says. Used for anything outside the core. */
data class ComingSoon(
    val title: String,
    val detail: String? = null,
    val owner: String? = null,
    val symbol: String = "sparkles",
    val hue: Hue = Hue.Sky,
    val kind: Kind = Kind.ServicesPhase,
) {
    enum class Kind { ServicesPhase, NotLiveYet, Maintenance, NotInDemo }

    val headline: String
        get() = when (kind) {
            Kind.ServicesPhase -> "This service comes in the next phase"
            Kind.NotLiveYet -> "Not available yet"
            Kind.Maintenance -> "Under maintenance"
            Kind.NotInDemo -> "Not part of this demo"
        }

    val message: String
        get() = when (kind) {
            Kind.ServicesPhase -> "This demo shows the core of the new DubaiNow. The steps inside each service are designed next, and they'll open from here."
            Kind.NotLiveYet -> "This service is planned. When it's ready it will appear here with everything you need to start."
            Kind.Maintenance -> "The service is being updated. You can still read about it and come back soon."
            Kind.NotInDemo -> "This part isn't built in the demo yet."
        }
}

/** Content of the "coming soon" bottom sheet. */
@Composable
fun ComingSoonContent(item: ComingSoon, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HueIcon(item.symbol, item.hue, size = 64.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, style = DNTextStyle.Title3.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
            item.owner?.let { Text(it, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.headline, style = DNTextStyle.Headline.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
            Text(item.detail ?: item.message, style = DNTextStyle.Body.style, color = DNColor.textSecondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.size(4.dp))
        DNButton("OK", onClick = onDone)
    }
}

@Composable
fun BoldText(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, style = DNTextStyle.Headline.style.copy(fontWeight = FontWeight.Bold))

@Composable
fun FillBox(content: @Composable BoxScope.() -> Unit) = Box(Modifier.fillMaxWidth(), content = content)
