package com.salehtl.dubainownext.features.welcome

import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNLogo
import com.salehtl.dubainownext.design.components.HueIcon
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Pre-login Welcome (Paper C1 · "10 · Welcome"): the whole screen is a story player that tours the app and carries
 * announcements; the sign-in panel underneath never moves, so nobody has to watch to get in.
 *
 * Rules (Paper board "Stories first, then one tap to sign in"):
 * - 6 s per story, the segment fills in a straight line. Tap the right two-thirds for next, the left third for back,
 *   or swipe sideways. Touch and hold pauses. It never loops on its own.
 * - Remove animations or TalkBack: no auto-advance; the story area is one element with next/previous actions.
 * - Larger text: the hero hides at large font sizes and the copy scrolls. Copy and panel show in full.
 *
 * Debug extras: `welcomeStory 1…6`, `welcomeSheet signIn|handoff|picker|verifying|verified|createAccount|events`,
 * `welcomeAutoSignIn true`, `welcomeReduceMotion true`, `welcomeTalkBack true`, `welcomeFontScale 2`, `welcomePaused true`.
 */
@Composable
fun WelcomeScreen() {
    // English-only build: Welcome stays left to right even on a phone set to Arabic. The heroes are placed from
    // Paper's English masters, and the progress fill and tap zones follow them. Mirror all of it when Arabic arrives.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        WelcomeDebugFontScale { WelcomePlayer() }
    }
}

/**
 * Debug `welcomeFontScale 2`: simulates the system font size setting. Welcome's sheets are their own windows (they
 * get the real density back), so each sheet applies it again.
 */
@Composable
internal fun WelcomeDebugFontScale(content: @Composable () -> Unit) {
    val override = remember { LaunchOptions.extras["welcomeFontScale"]?.toFloatOrNull() }
    if (override == null) {
        content()
    } else {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, override), content = content)
    }
}

@Composable
private fun WelcomePlayer() {
    val store = LocalStore.current
    val view = LocalView.current
    val launch = remember { WelcomeLaunch.consume() }
    val stories = WelcomeStory.all
    // The story and an open sheet survive rotation (the activity is recreated).
    var savedStory by rememberSaveable { mutableIntStateOf(launch.story) }
    val clock = remember { WelcomeStoryClock(stories.size, savedStory) }
    LaunchedEffect(clock.index) { savedStory = clock.index }
    var sheet by rememberSaveable { mutableStateOf(launch.sheet) }
    var signInStage by remember { mutableStateOf(launch.signInStage) }
    var autoSignIn by remember { mutableStateOf(launch.autoSignIn) }
    var isHolding by remember { mutableStateOf(false) }
    val debugPaused = remember { LaunchOptions.flag("welcomePaused") }

    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val reduceMotion = rememberWelcomeReduceMotion(resumed)
    val talkBack = rememberWelcomeTalkBack()
    // Remove animations and TalkBack turn auto-advance off.
    val autoAdvance = !reduceMotion && !talkBack
    val shouldRun = autoAdvance && !debugPaused && !isHolding && sheet == null && resumed && !clock.isFinished

    // The story timer: a frame loop adds played time while the story runs; pausing keeps it.
    LaunchedEffect(clock.index, shouldRun) {
        if (!shouldRun) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            clock.elapsedMs += (now - last) / 1_000_000f
            last = now
            if (clock.elapsedMs >= WelcomeStoryClock.DURATION_MS) {
                clock.timerEnded()
                break
            }
        }
    }

    val story = stories[clock.index]
    fun goNext() {
        DNHaptics.selection(view)
        clock.next()
    }
    fun goPrevious() {
        DNHaptics.selection(view)
        clock.previous()
    }

    CompositionLocalProvider(LocalWelcomeReduceMotion provides reduceMotion) {
        Box(Modifier.fillMaxSize()) {
            WelcomeWash(story.hue, reduceMotion)
            Column(Modifier.fillMaxSize()) {
                WelcomeStoryArea(
                    story = story,
                    clock = clock,
                    autoAdvance = autoAdvance,
                    reduceMotion = reduceMotion,
                    onNext = ::goNext,
                    onPrevious = ::goPrevious,
                    onHold = { isHolding = it },
                    onEvents = { sheet = WelcomeSheet.Events },
                    modifier = Modifier.weight(1f),
                )
                WelcomePanel(
                    onSignIn = { sheet = WelcomeSheet.SignIn },
                    onGuest = { store.continueAsGuest() },
                    onCreateAccount = { sheet = WelcomeSheet.CreateAccount },
                )
            }
        }

        // Sheets are local: Welcome sits outside the main scaffold and its router.
        fun closeSheet() {
            sheet = null
            signInStage = null
            autoSignIn = false
        }
        when (sheet) {
            WelcomeSheet.SignIn -> WelcomeSignInSheet(
                debugStage = signInStage,
                autoSignIn = autoSignIn,
                onDismiss = ::closeSheet,
                onSignedIn = { persona ->
                    closeSheet()
                    store.signIn(persona)
                },
            )
            WelcomeSheet.CreateAccount -> WelcomeComingSoonSheet(
                ComingSoon(
                    title = "Create a UAE PASS account",
                    detail = "In the real app, this opens UAE PASS so you can create your account. When it’s done, you come back to DubaiNow signed in.",
                    owner = "UAE PASS",
                    symbol = "person.crop.circle.badge.plus",
                    hue = Hue.Sky,
                    kind = ComingSoon.Kind.NotInDemo,
                ),
                icon = Icons.Rounded.PersonAdd,
                onDismiss = ::closeSheet,
            )
            WelcomeSheet.Events -> WelcomeComingSoonSheet(
                ComingSoon(
                    title = "Dubai Fitness Challenge events",
                    detail = "In the real app, this link opens the events, and you don’t need to sign in to see them.",
                    owner = "Dubai Fitness Challenge",
                    symbol = "figure.run",
                    hue = Hue.Rose,
                    kind = ComingSoon.Kind.NotInDemo,
                ),
                icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                onDismiss = ::closeSheet,
            )
            null -> Unit
        }
    }
}

// MARK: - Accessibility settings

/** The system "Remove animations" setting (animator duration scale 0), re-read when the app comes back. */
@Composable
private fun rememberWelcomeReduceMotion(resumed: Boolean): Boolean {
    val context = LocalContext.current
    return remember(resumed) {
        LaunchOptions.flag("welcomeReduceMotion") || runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/** TalkBack (touch exploration) on, following changes while Welcome is up. */
@Composable
private fun rememberWelcomeTalkBack(): Boolean {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(AccessibilityManager::class.java) }
    var on by remember { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { on = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return on || LaunchOptions.flag("welcomeTalkBack")
}

/**
 * Caps the font scale for chrome that must stay on one line (iOS caps the header at accessibility1). With max 1 it
 * keeps marks with text inside a fixed shape (the UAE PASS badge, avatar initials) at their drawn size.
 */
@Composable
internal fun WelcomeFontScaleCap(max: Float, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    if (d.fontScale <= max) content() else CompositionLocalProvider(LocalDensity provides Density(d.density, max), content = content)
}

// MARK: - Wash

/** The story's hue wash: start → end (62%) → start, top to bottom. Cross-fades in 300 ms (250 ms reduced). */
@Composable
private fun WelcomeWash(hue: Hue, reduceMotion: Boolean) {
    val spec = tween<Color>(if (reduceMotion) 250 else 300)
    val start by animateColorAsState(hue.start, spec, label = "wash-start")
    val end by animateColorAsState(hue.end, spec, label = "wash-end")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to start, 0.62f to end, 1f to start)))
}

// MARK: - Story area

@Composable
private fun WelcomeStoryArea(
    story: WelcomeStory,
    clock: WelcomeStoryClock,
    autoAdvance: Boolean,
    reduceMotion: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onHold: (Boolean) -> Unit,
    onEvents: () -> Unit,
    modifier: Modifier,
) {
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    val hold by rememberUpdatedState(onHold)
    val large = LocalDensity.current.fontScale >= 1.5f
    val count = clock.count

    Box(
        modifier
            .fillMaxWidth()
            // Only the story area takes the story gestures; the panel never does. The "See events" sticker
            // consumes its own taps, so they never also change the story.
            .pointerInput(Unit) {
                val swipe = 44.dp.toPx()
                val slop = 12.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.isConsumed) return@awaitEachGesture
                    hold(true)
                    var last: PointerInputChange = down
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            last = change
                            if (!change.pressed) break
                        }
                    } finally {
                        // Letting go after a hold just resumes the story (also if the gesture is cut short).
                        hold(false)
                    }
                    if (last.isConsumed) return@awaitEachGesture
                    val dx = last.position.x - down.position.x
                    val dy = last.position.y - down.position.y
                    val held = last.uptimeMillis - down.uptimeMillis
                    if (abs(dx) > swipe && abs(dx) > abs(dy)) {
                        if (dx < 0) next() else previous()
                    } else if (held < 350 && hypot(dx, dy) < slop) {
                        if (down.position.x < size.width / 3f) previous() else next()
                    }
                }
            }
            // TalkBack: one element ("Story 2 of 6, What needs you, first.") with next/previous actions.
            .clearAndSetSemantics {
                contentDescription = "Story ${clock.index + 1} of $count, ${story.headline}"
                stateDescription = story.accessibilityValue
                // Read the new story when it changes. With TalkBack it only changes through these actions
                // (auto-advance is off), so this never talks over anything.
                liveRegion = LiveRegionMode.Polite
                // Double-tap works like a tap on the right: next, and from the last story back to story 1.
                onClick(label = if (clock.index < count - 1) "Next story" else "Back to story 1") { onNext(); true }
                customActions = listOfNotNull(
                    // Like VoiceOver's adjustable element: next stops at the last story, previous at the first.
                    if (clock.index < count - 1) CustomAccessibilityAction("Next story") { onNext(); true } else null,
                    if (clock.index > 0) CustomAccessibilityAction("Previous story") { onPrevious(); true } else null,
                    if (story.hero == WelcomeHeroKind.Poster) CustomAccessibilityAction("See events") { onEvents(); true } else null,
                )
            },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            WelcomeProgressBar(
                count = count,
                index = clock.index,
                finished = clock.isFinished,
                // Without auto-advance the current segment just shows where you are.
                fraction = { if (autoAdvance) clock.fraction else 1f },
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
            )
            WelcomeFontScaleCap(1.3f) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp).heightIn(min = 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Crossfade(story.source, animationSpec = tween(if (reduceMotion) 250 else 300), label = "welcome-header") { source ->
                        WelcomeStoryHeader(source, story.hue)
                    }
                    Spacer(Modifier.weight(1f))
                    // English only for this build: the عربي language pill (top end in Paper; it switches the whole
                    // app at once and keeps the current story) is hidden. Put it back here with Arabic.
                }
            }
            if (large) {
                // The hero hides at large font sizes; the copy scrolls if it has to.
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(24.dp))
                    WelcomeCopySwitcher(story, reduceMotion)
                }
            } else {
                WelcomeHeroArea(story, reduceMotion, onEvents, Modifier.weight(1f))
                WelcomeCopySwitcher(story, reduceMotion)
            }
        }
    }
}

/** Six 3 dp segments: played stories full, the current one filling, the rest empty. */
@Composable
private fun WelcomeProgressBar(count: Int, index: Int, finished: Boolean, fraction: () -> Float, modifier: Modifier) {
    Row(modifier.fillMaxWidth().height(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { i ->
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(DNColor.progressTrackOnHue, cornerRadius = r)
                // Read the fraction here (draw phase) so the frame loop only redraws the bar.
                val fill = if (finished || i < index) 1f else if (i == index) fraction() else 0f
                if (fill > 0f) drawRoundRect(DNColor.textPrimary, size = Size(size.width * fill, size.height), cornerRadius = r)
            }
        }
    }
}

/** DubaiNow · Welcome, or the entity that published the announcement. */
@Composable
private fun WelcomeStoryHeader(source: WelcomeSource, hue: Hue) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val style = DNTextStyle.Subheadline.style
        when (source) {
            WelcomeSource.DubaiNow -> {
                Box(
                    Modifier.size(36.dp).shadow(1.dp, CircleShape).clip(CircleShape).background(DNColor.surfacePrimary),
                    contentAlignment = Alignment.Center,
                ) { DNLogo(height = 19.dp) }
                Text("DubaiNow", style = style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary, maxLines = 1)
                Text("Welcome", style = style, color = DNColor.textOnWashSecondary, maxLines = 1)
            }
            is WelcomeSource.Entity -> {
                Box(
                    Modifier.size(36.dp).shadow(1.dp, CircleShape).clip(CircleShape).background(hue.ink),
                    contentAlignment = Alignment.Center,
                ) { Icon(source.icon, contentDescription = null, tint = DNColor.white, modifier = Modifier.size(20.dp)) }
                Text(source.name, style = style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary, maxLines = 1)
                Text(source.age, style = style, color = DNColor.textOnWashSecondary, maxLines = 1)
            }
        }
    }
}

/** The hero keeps Paper's composition and scales down to fit; it hides when it would get too small. */
@Composable
private fun WelcomeHeroArea(story: WelcomeStory, reduceMotion: Boolean, onEvents: () -> Unit, modifier: Modifier) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val scale = minOf(1f, maxWidth / WelcomeHeroWidth, maxHeight / WelcomeHeroHeight)
        if (scale >= 0.5f) {
            // Paper's hero frame is 419 tall with the objects in its top 392: sit at the top like Paper and
            // centre only the space beyond that on taller phones.
            val top = ((maxHeight - WelcomeHeroHeight * scale - 27.dp) / 2).coerceAtLeast(0.dp)
            // The old hero fades out (300 ms; the 250 ms cross-fade with Remove animations); the new one rises.
            val fade = if (reduceMotion) 250 else 300
            AnimatedContent(
                targetState = story,
                transitionSpec = { EnterTransition.None togetherWith fadeOut(tween(fade)) using SizeTransform(clip = false) },
                contentKey = { it.id },
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxWidth().padding(top = top),
                label = "welcome-hero",
            ) { s ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.welcomeScaledCanvas(scale)) {
                        // Hero text is part of the picture: fixed size, whatever the font setting. The scale lets
                        // the "See events" sticker keep a 48 dp touch area on screen.
                        CompositionLocalProvider(
                            LocalDensity provides Density(density.density, 1f),
                            LocalWelcomeHeroScale provides scale,
                        ) {
                            WelcomeHero(s.hero, onEvents)
                        }
                    }
                }
            }
        }
    }
}

/** Measures the hero at its Paper size (402 × 392) and draws it scaled, taking only the scaled space. */
private fun Modifier.welcomeScaledCanvas(scale: Float): Modifier = layout { measurable, _ ->
    val w = WelcomeHeroWidth.roundToPx()
    val h = WelcomeHeroHeight.roundToPx()
    val placeable = measurable.measure(Constraints.fixed(w, h))
    val sw = (w * scale).roundToInt()
    val sh = (h * scale).roundToInt()
    layout(sw, sh) {
        placeable.placeWithLayer((sw - w) / 2, (sh - h) / 2) {
            scaleX = scale
            scaleY = scale
        }
    }
}

/** Kicker, one-line headline (34/40 bold) and body. Copy fades in last. */
@Composable
private fun WelcomeCopySwitcher(story: WelcomeStory, reduceMotion: Boolean) {
    AnimatedContent(
        targetState = story,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(250)) togetherWith fadeOut(tween(250)) using SizeTransform(clip = false)
            } else {
                fadeIn(tween(250, delayMillis = 180)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false)
            }
        },
        contentKey = { it.id },
        contentAlignment = Alignment.BottomStart,
        label = "welcome-copy",
    ) { s ->
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(s.kicker.uppercase(), style = DNTextStyle.Overline.style, color = s.hue.ink)
            Text(s.headline, style = DNTextStyle.LargeTitle.style, color = DNColor.textPrimary)
            Text(s.body, style = DNTextStyle.Body.style, color = DNColor.textOnWashSecondary)
        }
    }
}

// MARK: - Sign-in panel (fixed: it doesn't advance with the stories)

@Composable
private fun WelcomePanel(onSignIn: () -> Unit, onGuest: () -> Unit, onCreateAccount: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = DNSpace.insetPage)
            .padding(bottom = 4.dp),
    ) {
        // UAE PASS is a brand button (Paper exception X-07): black, its mark and label, our pill, Large height.
        WelcomePill(fill = Color.Black, ripple = DNColor.white, onClick = onSignIn) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = DNColor.white, modifier = Modifier.size(26.dp))
            Spacer(Modifier.size(DNSpace.xs))
            Text(
                "Sign in with UAE PASS",
                style = DNTextStyle.Button.style, color = DNColor.white, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        WelcomePill(fill = DNColor.surfacePrimary, ripple = DNColor.textPrimary, onClick = onGuest) {
            Text(
                "Continue as guest",
                style = DNTextStyle.Button.style, color = DNColor.textPrimary, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = DNSpace.targetMinimum)
                .clip(RoundedCornerShape(DNRadius.control))
                .clickable(role = Role.Button, onClick = onCreateAccount),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                buildAnnotatedString {
                    append("New to UAE PASS? ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = DNColor.textOnWashLink)) { append("Create an account") }
                },
                style = DNTextStyle.Subheadline.style,
                color = DNColor.textOnWashSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

/** The two panel pills: 56 dp (Android Large), ripple instead of the iOS press scale. Labels wrap, never truncate. */
@Composable
private fun WelcomePill(fill: Color, ripple: Color, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(interactionSource = null, indication = ripple(color = ripple), role = Role.Button, onClick = onClick)
            .padding(horizontal = DNSpace.md, vertical = DNSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

// MARK: - Coming soon sheets (create an account, the events link)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeComingSoonSheet(item: ComingSoon, icon: ImageVector, onDismiss: () -> Unit) {
    // Opens in full: with larger text a half-height sheet would hide the OK button below the screen.
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = DNColor.surfacePrimary,
        shape = RoundedCornerShape(topStart = DNRadius.sheet, topEnd = DNRadius.sheet),
    ) {
        WelcomeDebugFontScale {
            Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
                WelcomeComingSoonContent(item, icon) {
                    scope.launch { state.hide() }.invokeOnCompletion { if (!state.isVisible) onDismiss() }
                }
            }
        }
    }
}

/**
 * Same layout as the shared ComingSoonContent, with a Material icon passed in (the shared symbol map has no
 * "person.crop.circle.badge.plus" or "figure.run" yet; see NOTES.md).
 */
@Composable
private fun WelcomeComingSoonContent(item: ComingSoon, icon: ImageVector, onDone: () -> Unit) {
    Column(
        // Scrolls when larger text makes it taller than the screen, so OK is always reachable.
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HueIcon(icon, item.hue, size = 64.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.title, style = DNTextStyle.Title3.style, color = DNColor.textPrimary, textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
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
