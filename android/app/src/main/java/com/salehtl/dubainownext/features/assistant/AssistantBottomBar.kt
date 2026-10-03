package com.salehtl.dubainownext.features.assistant

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.TonalIconButton
import kotlin.math.sin

private enum class AssistantAbove { Transcript, Caption, Suggestions, Nothing }

/**
 * Your side (the bottom): up to three suggestion chips above the ask bar; while it listens, the
 * transcript; when it needs you, the one confirm under your thumb (Paper RUW-0, SVR-0, TQX-0, UXH-0).
 * Swipe the bar down to close.
 */
@Composable
internal fun AssistantBottomBar(
    engine: AssistantEngine,
    suggestions: List<AssistantSuggestion>,
    reduceMotion: Boolean,
    focusRequester: FocusRequester,
    onFieldFocus: (Boolean) -> Unit,
    onDismissKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirm = engine.confirm
    val above = when {
        engine.phase == AssistantPhase.Listening -> AssistantAbove.Transcript
        confirm != null -> AssistantAbove.Caption
        suggestions.isNotEmpty() -> AssistantAbove.Suggestions
        else -> AssistantAbove.Nothing
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedContent(above to suggestions, transitionSpec = { fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.fast()) }, label = "above") { (kind, chips) ->
            when (kind) {
                AssistantAbove.Transcript -> AssistantTranscript(engine.heard, engine.hearing)
                AssistantAbove.Caption -> engine.confirm?.let { AssistantConfirmCaption(engine, it) }
                AssistantAbove.Suggestions -> AssistantSuggestionChips(chips, reduceMotion) { s ->
                    onDismissKeyboard()
                    engine.run(s.text)
                }
                AssistantAbove.Nothing -> Box(Modifier.fillMaxWidth())
            }
        }
        // The bar: close + ask bar, or close + the confirm.
        Row(
            Modifier.fillMaxWidth().pointerInput(Unit) {
                // Swipe down to close. Measured from touch-down to lift-off like iOS's translation
                // (> 60 pt), but the bar sits only ~64 dp above the screen edge here: 40 dp, or a
                // quick flick, is enough.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val velocity = VelocityTracker().apply { addPointerInputChange(down) }
                    val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
                    velocity.addPointerInputChange(drag)
                    var y = drag.position.y
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == drag.id } ?: return@awaitEachGesture
                        velocity.addPointerInputChange(change)
                        y = change.position.y
                        if (!change.pressed) break
                        if (change.isConsumed) return@awaitEachGesture
                        change.consume()
                    }
                    val dy = y - down.position.y
                    val flick = dy > 20.dp.toPx() && velocity.calculateVelocity().y > 1000.dp.toPx()
                    if (dy > 40.dp.toPx() || flick) engine.close()
                }
            },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TonalIconButton(
                Icons.Rounded.Close, if (confirm == null) "Close the assistant" else "Not now. Nothing is paid or sent",
                onClick = { engine.close() }, size = 56.dp, container = DNColor.surfacePrimary,
            )
            AnimatedContent(confirm, contentKey = { it?.title }, transitionSpec = {
                (fadeIn(DNMotion.base()) + scaleIn(DNMotion.smooth(), initialScale = 0.96f)) togetherWith fadeOut(DNMotion.fast())
            }, modifier = Modifier.weight(1f), label = "bar") { c ->
                if (c != null) {
                    AssistantConfirmButton(c, engine.showsEnglish, engine.isConfirming, reduceMotion) { engine.confirmTapped() }
                } else {
                    AssistantAskBar(engine, reduceMotion, focusRequester, onFieldFocus, onDismissKeyboard)
                }
            }
        }
    }
}

@Composable
private fun AssistantTranscript(heard: String, hearing: String) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = DNColor.textPrimary)) { append(heard) }
                withStyle(SpanStyle(color = DNColor.neutral400)) { append(hearing) }
            },
            style = DNTextStyle.Title1.style,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = heard.ifEmpty { "Listening" } },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(DNColor.actionAccent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(DNColor.actionAccent))
            }
            Text("Listening · let go when you're done", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textSecondary)
        }
    }
}

@Composable
private fun AssistantConfirmCaption(engine: AssistantEngine, confirm: AssistantConfirm) {
    // Centred over the confirm button (the close button takes 56 + 10 on the left).
    Box(Modifier.fillMaxWidth().padding(start = 66.dp), contentAlignment = Alignment.Center) {
        if (confirm.isUaePass) {
            Text(confirm.caption?.resolved(engine.showsEnglish) ?: "", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Wraps (instead of pushing "Change" off screen) with large text.
                Text("With ${engine.paymentMethod.label} ·", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textSecondary,
                    modifier = Modifier.weight(1f, fill = false))
                Box(
                    Modifier.heightIn(min = 48.dp).clip(CircleShape)
                        .clickable(enabled = engine.canChangeMethod, role = Role.Button) { engine.changeMethod() }
                        .clearAndSetSemantics { contentDescription = "Change how you pay, now ${engine.paymentMethod.label}" }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Change", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink) }
            }
        }
    }
}

/** Suggestions: M3 assist chips (elevated, so they read on the veil), glyph in the topic's ink. */
@Composable
private fun AssistantSuggestionChips(suggestions: List<AssistantSuggestion>, reduceMotion: Boolean, onTap: (AssistantSuggestion) -> Unit) {
    val view = LocalView.current
    Column(Modifier.fillMaxWidth().semantics { contentDescription = "Suggestions" }, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        suggestions.forEachIndexed { i, s ->
            val appear = remember(s.text) { Animatable(if (reduceMotion) 1f else 0f) }
            LaunchedEffect(s.text) {
                kotlinx.coroutines.delay(DNMotion.STAGGER_MS * i)
                appear.animateTo(1f, DNMotion.smooth())
            }
            ElevatedAssistChip(
                onClick = { DNHaptics.selection(view); onTap(s) },
                label = {
                    Text(s.text, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                leadingIcon = { Icon(assistantIcon(s.symbol), contentDescription = null, tint = s.hue.ink, modifier = Modifier.size(18.dp)) },
                colors = AssistChipDefaults.elevatedAssistChipColors(containerColor = DNColor.surfacePrimary),
                elevation = AssistChipDefaults.elevatedAssistChipElevation(elevation = 2.dp),
                modifier = Modifier.heightIn(min = 40.dp).semantics { role = Role.Button }.graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * 12.dp.toPx()
                },
            )
        }
    }
}

@Composable
private fun AssistantAskBar(
    engine: AssistantEngine,
    reduceMotion: Boolean,
    focusRequester: FocusRequester,
    onFieldFocus: (Boolean) -> Unit,
    onDismissKeyboard: () -> Unit,
) {
    val placeholder = if (engine.card == null) "Ask or tell me what to do" else "Ask or add something"
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .shadow(6.dp, CircleShape, ambientColor = DNColor.blue950.copy(alpha = 0.2f), spotColor = DNColor.blue950.copy(alpha = 0.2f))
            .clip(CircleShape).background(DNColor.surfacePrimary)
            .padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (engine.phase == AssistantPhase.Listening) {
            AssistantWaveform(reduceMotion, Modifier.weight(1f))
            AssistantRoundButton("arrow.up", "Send") { engine.sendListening() }
        } else {
            BasicTextField(
                value = engine.draft,
                onValueChange = { engine.draft = it },
                singleLine = true,
                textStyle = DNTextStyle.Body.style.copy(color = DNColor.textPrimary),
                cursorBrush = SolidColor(DNColor.actionAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onDismissKeyboard(); engine.sendDraft() }),
                modifier = Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged { onFieldFocus(it.isFocused) }
                    .semantics { contentDescription = placeholder },
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                        // The field's own label says it once (TalkBack read the placeholder twice).
                        if (engine.draft.isEmpty()) Text(placeholder, style = DNTextStyle.Body.style, color = DNColor.textSecondary, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics {})
                        inner()
                    }
                },
            )
            if (engine.draft.isBlank()) {
                AssistantRoundButton("mic.fill", "Talk. Say what you need") { onDismissKeyboard(); engine.listen() }
            } else {
                AssistantRoundButton("arrow.up", "Send") { onDismissKeyboard(); engine.sendDraft() }
            }
        }
    }
}

/** The mic / send circle: 40 dp accent disc in a 48 dp target. */
@Composable
private fun AssistantRoundButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(DNColor.actionAccent), contentAlignment = Alignment.Center) {
            Icon(assistantIcon(symbol), contentDescription = null, tint = DNColor.textOnAccent, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * The confirm: "Pay AED 1,020" (accent) or "Confirm with UAE PASS" (black), both with the
 * fingerprint glyph. While confirming: "Confirming…" and the glyph pulses.
 */
@Composable
private fun AssistantConfirmButton(confirm: AssistantConfirm, english: Boolean, isConfirming: Boolean, reduceMotion: Boolean, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(1f, 0.35f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "p")
    val title = confirm.title.resolved(english)
    val bg = if (confirm.isUaePass) SolidColor(DNColor.ink950) else Brush.verticalGradient(listOf(DNColor.blue600, DNColor.blue700))
    val shadow = if (confirm.isUaePass) DNColor.ink950 else DNColor.actionAccent
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .shadow(10.dp, CircleShape, ambientColor = shadow, spotColor = shadow)
            .clip(CircleShape).background(bg)
            .clickable(enabled = !isConfirming, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = if (confirm.isUaePass) "$title. ${confirm.caption?.resolved(english) ?: "It's sent in your name"}" else "$title. Confirms with your fingerprint"
                if (isConfirming) stateDescription = if (english) "Confirming" else "确认中"
            }
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            assistantIcon("touchid"), contentDescription = null, tint = Color.White,
            modifier = Modifier.size(24.dp).alpha(if (isConfirming && !reduceMotion) pulse else 1f),
        )
        Text(
            if (isConfirming) (if (english) "Confirming…" else "确认中…") else title,
            style = DNTextStyle.Button.style.copy(textAlign = TextAlign.Center), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/** Listening: the voice as bars; heard bars in the accent, the rest as dots. */
@Composable
private fun AssistantWaveform(reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val heights = remember { listOf(4f, 8f, 14f, 22f, 16f, 10f, 18f, 26f, 20f, 12f, 8f, 16f, 22f, 14f, 8f, 12f, 18f, 10f) }
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { clock.floatValue = (it - start) / 1_000_000_000f }
    }
    Canvas(modifier.height(28.dp)) {
        val t = clock.floatValue
        val bar = 3.dp.toPx(); val gap = 4.dp.toPx()
        for (i in 0 until 28) {
            val x = i * (bar + gap)
            if (x + bar > size.width) break
            val lit = i < 18
            val base = if (lit) heights[i % heights.size] else 4f
            val wobble = if (lit && !reduceMotion) 0.65f + 0.35f * sin(t * 9f + i * 0.8f) else 1f
            val h = maxOf(4f, base * wobble).dp.toPx()
            drawRoundRect(
                if (lit) DNColor.actionAccent else DNColor.neutral400,
                topLeft = Offset(x, (size.height - h) / 2), size = Size(bar, h), cornerRadius = CornerRadius(bar / 2),
            )
        }
    }
}
