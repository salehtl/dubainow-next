package com.salehtl.dubainownext.app

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.Inter
import com.salehtl.dubainownext.design.components.Avatar
import com.salehtl.dubainownext.design.components.DNLogo
import com.salehtl.dubainownext.design.components.FixedFontScale
import com.salehtl.dubainownext.design.components.TonalIconButton

/**
 * The header on every main screen (Home, Services, Activity tab roots): Happiness Meter smiley
 * top left (never a bell), the DubaiNow logo top centre (tap → assistant, touch and hold → talk),
 * bell (Home only) and avatar top right. Android: tonal circles instead of glass.
 */
@Composable
fun MainHeader(modifier: Modifier = Modifier, showsBell: Boolean = false) {
    val store = LocalStore.current; val router = LocalRouter.current
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Both sides are the same width (two buttons) so the logo sits in the true centre (Paper 9U5).
        Row(Modifier.width(HeaderSideWidth)) {
            TonalIconButton(Icons.Rounded.SentimentSatisfied, "Happiness Meter", onClick = { router.present(SheetRoute.HappinessMeter()) })
        }
        Spacer(Modifier.weight(1f))
        AssistantLogoButton()
        Spacer(Modifier.weight(1f))
        Row(Modifier.width(HeaderSideWidth), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            if (showsBell && !store.isGuest) {
                Box {
                    TonalIconButton(Icons.Rounded.NotificationsNone,
                        if (store.unreadCount > 0) "Notifications, ${store.unreadCount} new" else "Notifications",
                        onClick = { router.present(SheetRoute.Notifications) })
                    if (store.unreadCount > 0) {
                        FixedFontScale {
                            Box(
                                Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                                    .clip(CircleShape).background(DNColor.red700).padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("${store.unreadCount}", color = DNColor.white, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Inter) }
                        }
                    }
                }
            }
            // Signing out fades the app out with the resident's avatar still on it.
            val lastPersona = remember { arrayOf(store.persona) }
            val p = store.persona?.also { lastPersona[0] = it } ?: lastPersona[0].takeIf { store.session == null }
            TonalIconButton(Icons.Rounded.PersonOutline, if (store.isGuest) "Sign in or profile" else "Your profile",
                onClick = { router.present(SheetRoute.Profile) },
                content = p?.let { { Avatar(it.initials, it.avatarHue, size = 36.dp) } })
        }
    }
}

private val HeaderSideWidth = 104.dp // two 48 dp buttons and an 8 dp gap

/** Pushed screens: back · logo · (actions). The assistant is reachable from every full screen. */
@Composable
fun PushedHeader(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    val router = LocalRouter.current
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Same-width sides as MainHeader, so the logo stays centred (where the assistant's face opens) with 0–2 actions.
        Row(Modifier.width(HeaderSideWidth)) {
            TonalIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = { onBack?.invoke() ?: router.pop() })
        }
        Spacer(Modifier.weight(1f))
        AssistantLogoButton()
        Spacer(Modifier.weight(1f))
        Row(Modifier.width(HeaderSideWidth), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = actions)
    }
}

/** The logo is the assistant's face. Tap to ask; touch and hold to talk. */
@Composable
fun AssistantLogoButton(modifier: Modifier = Modifier) {
    val store = LocalStore.current; val router = LocalRouter.current; val view = LocalView.current
    val signedIn = store.session != null && !store.isGuest
    Box(
        modifier.size(56.dp).clip(CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button,
                onLongClick = { if (signedIn) { DNHaptics.medium(view); router.dismissSheet(); router.openAssistant("__listen__") } },
                onClick = { if (signedIn) { DNHaptics.medium(view); router.dismissSheet(); router.openAssistant() } },
            )
            .semantics { contentDescription = "DubaiNow assistant" },
        contentAlignment = Alignment.Center,
    ) { DNLogo(height = 28.dp) }
}
