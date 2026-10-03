package com.salehtl.dubainownext.features.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.FaceGlyph
import com.salehtl.dubainownext.design.components.HappinessFace
import com.salehtl.dubainownext.design.components.HappinessFacesPicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Happiness Meter · Survey sheet (Paper C1 `F49-0`, rules `F97-0`), from the header smiley,
 * Profile, or with a service as context. One tap on a face is enough; reasons and a comment are
 * optional. After Unhappy it offers help. Closing after a face still thanks (the rating is sent).
 */
@Composable
fun HappinessMeterSheet(context: String?) {
    val router = LocalRouter.current
    ProfileHappinessContent(context, onClose = { router.dismissSheet() })
}

private enum class ProfileHappinessStage { Rating, Sent, UnhappyHelp }

/** Plain holder (not state) so the "thanks" decision survives the sheet being torn down. */
private class ProfileHappinessTracker { var face: HappinessFace? = null; var thanked = false }

private const val PROFILE_THANKS = "Thanks — your feedback goes to the Happiness Meter"

@Composable
internal fun ProfileHappinessContent(context: String?, onClose: () -> Unit) {
    val toasts = LocalToasts.current
    val view = LocalView.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // Demo hooks: `happinessFace happy|neutral|unhappy` preselects a face; `happinessSent true` shows after-send.
    val launchFace = remember { ProfileDebug.extra("happinessFace")?.let { f -> HappinessFace.entries.firstOrNull { it.name.equals(f, ignoreCase = true) } } }
    val launchSent = remember { launchFace != null && ProfileDebug.flag("happinessSent") }
    var face by remember { mutableStateOf(launchFace) }
    var reasons by remember { mutableStateOf(setOf<String>()) }
    var comment by remember { mutableStateOf("") }
    var stage by remember {
        mutableStateOf(
            when {
                !launchSent -> ProfileHappinessStage.Rating
                launchFace == HappinessFace.Unhappy -> ProfileHappinessStage.UnhappyHelp
                else -> ProfileHappinessStage.Sent
            },
        )
    }
    var comingSoon by remember { mutableStateOf<ComingSoon?>(null) }
    val tracker = remember { ProfileHappinessTracker().also { it.face = launchFace; it.thanked = launchSent && launchFace != HappinessFace.Unhappy } }

    // The rating is sent as soon as a face is tapped, so closing never loses it.
    DisposableEffect(Unit) { onDispose { if (tracker.face != null && !tracker.thanked) toasts.show(PROFILE_THANKS) } }

    fun closeWithThanks() {
        if (tracker.thanked) return
        tracker.thanked = true
        onClose()
        toasts.show(PROFILE_THANKS)
    }

    fun send() {
        focus.clearFocus()
        DNHaptics.success(view)
        if (face == HappinessFace.Unhappy) {
            stage = ProfileHappinessStage.UnhappyHelp
        } else {
            stage = ProfileHappinessStage.Sent
            scope.launch { delay(1200); closeWithThanks() }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        ProfileSheetTitleRow("Happiness Meter", onClose = onClose)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).animateContentSize(DNMotion.smooth())
                .padding(horizontal = DNSpace.insetPage).padding(bottom = 24.dp),
        ) {
            AnimatedContent(
                stage, label = "happiness-stage",
                transitionSpec = { fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.fast()) },
            ) { st ->
                when (st) {
                    ProfileHappinessStage.Rating -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            profileHappinessQuestion(context), style = DNTextStyle.Title2.style.copy(fontWeight = FontWeight.Bold),
                            color = DNColor.textPrimary, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { heading() },
                        )
                        HappinessFacesPicker(
                            selection = face,
                            onSelect = { f -> if (f != face) reasons = emptySet(); face = f; tracker.face = f },
                            size = 80.dp,
                        )
                        AnimatedContent(
                            face, label = "happiness-follow-up",
                            transitionSpec = { (fadeIn(DNMotion.base()) + slideInVertically(DNMotion.smooth()) { it / 10 }) togetherWith fadeOut(DNMotion.fast()) },
                        ) { f ->
                            if (f == null) {
                                Text(
                                    "One tap is enough. Goes to the Dubai Government Happiness Meter. Your name is not shared.",
                                    style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                ProfileHappinessFollowUp(
                                    f, reasons, comment,
                                    onToggle = { r -> DNHaptics.selection(view); reasons = if (r in reasons) reasons - r else reasons + r },
                                    onComment = { comment = it },
                                    onSend = ::send,
                                )
                            }
                        }
                    }
                    ProfileHappinessStage.Sent -> ProfileHappinessSent()
                    ProfileHappinessStage.UnhappyHelp -> ProfileHappinessHelp(
                        onHelp = {
                            comingSoon = ComingSoon(
                                title = "Get help with this",
                                detail = "Help pages, live chat and a call back come in the next phase of DubaiNow. Your rating was still sent.",
                                symbol = "questionmark.circle.fill", hue = Hue.Sky, kind = ComingSoon.Kind.NotInDemo,
                            )
                        },
                        onReport = {
                            comingSoon = ComingSoon(
                                title = "Report a problem",
                                detail = "Reporting a problem to the DubaiNow team comes in the next phase. Your rating and comment were still sent.",
                                symbol = "exclamationmark.bubble.fill", hue = Hue.Rose, kind = ComingSoon.Kind.NotInDemo,
                            )
                        },
                        onNotNow = ::closeWithThanks,
                    )
                }
            }
        }
    }
    ProfileComingSoonSheet(comingSoon) { comingSoon = null }
}

/** Tracking in em (0 sp becomes 0 em) so M3's label animation can interpolate it. */
private fun profileEm(t: TextUnit): TextUnit = if (t.isEm) t else 0.em

private fun profileHappinessQuestion(context: String?): String {
    if (context.isNullOrEmpty()) return "How do you feel about DubaiNow today?"
    val s = Catalogue.service(context)
    if (s != null) {
        return if (s.kind == "service") "How was it to ${ProfileText.lowerFirst(s.name)}?" else "How do you feel about ${ProfileText.lowerFirst(s.name)}?"
    }
    return "How was it to ${ProfileText.lowerFirst(context)}?"
}

private fun profileReasonsTitle(face: HappinessFace) = when (face) {
    HappinessFace.Happy -> "What went well?"
    HappinessFace.Neutral -> "What could be better?"
    HappinessFace.Unhappy -> "What went wrong?"
}

private fun profileReasonOptions(face: HappinessFace) = when (face) {
    HappinessFace.Happy -> listOf("Easy to find", "Quick", "Clear information", "Easy to pay", "Something else")
    HappinessFace.Neutral -> listOf("Finding a service", "Speed", "Paying", "Wrong information", "Something else")
    HappinessFace.Unhappy -> listOf("Something didn't work", "Too slow", "Couldn't find it", "Wrong information", "Paying", "Something else")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHappinessFollowUp(
    face: HappinessFace,
    reasons: Set<String>,
    comment: String,
    onToggle: (String) -> Unit,
    onComment: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(profileReasonsTitle(face), style = DNTextStyle.Headline.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            profileReasonOptions(face).forEach { r ->
                val selected = r in reasons
                FilterChip(
                    selected = selected, onClick = { onToggle(r) },
                    label = { Text(r, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold)) },
                    leadingIcon = if (selected) { { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) } } else null,
                    shape = CircleShape,
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DNColor.chipSurface, labelColor = DNColor.chipText,
                        selectedContainerColor = DNColor.chipSurfaceSelected, selectedLabelColor = DNColor.chipTextSelected,
                        selectedLeadingIconColor = DNColor.chipTextSelected,
                    ),
                    modifier = Modifier.heightIn(min = 40.dp),
                )
            }
        }
        // The label animates between bodyLarge and bodySmall; their tracking must share a unit (em),
        // or the lerp throws "Cannot perform operation for Em and Sp" (see NOTES.md, shared request).
        val typography = MaterialTheme.typography
        MaterialTheme(
            typography = typography.copy(
                bodyLarge = typography.bodyLarge.copy(letterSpacing = profileEm(typography.bodyLarge.letterSpacing)),
                bodySmall = typography.bodySmall.copy(letterSpacing = profileEm(typography.bodySmall.letterSpacing)),
            ),
        ) {
            OutlinedTextField(
                value = comment, onValueChange = onComment,
                label = { Text("Tell us more (optional)") },
                minLines = 3, maxLines = 6,
                textStyle = DNTextStyle.Body.style,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DNColor.fieldSurfaceOnGrouped, unfocusedContainerColor = DNColor.fieldSurfaceOnGrouped,
                    focusedBorderColor = DNColor.fieldFocus, unfocusedBorderColor = DNColor.borderControl,
                    focusedLabelColor = DNColor.fieldFocus, unfocusedLabelColor = DNColor.fieldHint,
                    cursorColor = DNColor.fieldFocus, focusedTextColor = DNColor.fieldText, unfocusedTextColor = DNColor.fieldText,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        DNButton("Send", onClick = onSend, modifier = Modifier.padding(top = 6.dp))
        Text(
            "Goes to the Dubai Government Happiness Meter. Your name is not shared.",
            style = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.Normal), color = DNColor.textSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileHappinessSent() {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, DNMotion.bouncy()) }
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            Icons.Rounded.CheckCircle, contentDescription = null, tint = DNColor.statusSuccessForeground,
            modifier = Modifier.size(64.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        )
        Text("Thanks", style = DNTextStyle.Title2.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary)
        Text("Your feedback goes to the Happiness Meter.", style = DNTextStyle.Body.style, color = DNColor.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileHappinessHelp(onHelp: () -> Unit, onReport: () -> Unit, onNotNow: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(HappinessFace.Unhappy.fill), contentAlignment = Alignment.Center) {
                FaceGlyph(HappinessFace.Unhappy, size = 30.dp)
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sorry about that", style = DNTextStyle.Title3.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                Text("Your rating was sent. Do you want help with it now?", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DNButton("Get help with this", onClick = onHelp)
            DNButton("Report a problem", onClick = onReport, kind = DNButtonKind.Secondary)
            DNButton("Not now", onClick = onNotNow, kind = DNButtonKind.Tertiary)
        }
    }
}
