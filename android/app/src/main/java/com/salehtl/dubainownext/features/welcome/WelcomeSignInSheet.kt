package com.salehtl.dubainownext.features.welcome

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.salehtl.dubainownext.data.Persona
import com.salehtl.dubainownext.data.personas.Personas
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.Avatar
import com.salehtl.dubainownext.design.components.DNLogo
import com.salehtl.dubainownext.design.components.TonalIconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs

/** Where the sign-in sheet is. */
private sealed interface WelcomeSignInStage {
    data object Handoff : WelcomeSignInStage
    data object Picker : WelcomeSignInStage
    data class Verifying(val persona: Persona) : WelcomeSignInStage
    data class Verified(val persona: Persona) : WelcomeSignInStage
}

/**
 * "Sign in with UAE PASS" in the demo, as our own M3 modal bottom sheet (Welcome sits outside the main scaffold):
 * a calm hand-off, then "Choose a demo resident", then a short biometric check. Neutral DubaiNow styling —
 * it doesn't imitate the UAE PASS app. [onSignedIn] runs once the sheet has slid away.
 *
 * [debugStage] (`welcomeSheet handoff|picker|verifying|verified`) opens on a stage; handoff/verifying/verified hold
 * still for screenshots. [autoSignIn] picks the first resident on its own (`welcomeAutoSignIn true`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WelcomeSignInSheet(
    debugStage: String?,
    autoSignIn: Boolean,
    onDismiss: () -> Unit,
    onSignedIn: (Persona) -> Unit,
) {
    val sample = Personas.all.first()
    val stage = remember {
        mutableStateOf(
            when (debugStage) {
                "picker" -> WelcomeSignInStage.Picker
                "verifying" -> WelcomeSignInStage.Verifying(sample)
                "verified" -> WelcomeSignInStage.Verified(sample)
                else -> WelcomeSignInStage.Handoff
            },
        )
    }
    val holdsStage = debugStage == "handoff" || debugStage == "verifying" || debugStage == "verified"
    val finishing = remember { mutableStateOf(false) }
    fun confirming() = stage.value is WelcomeSignInStage.Verifying || stage.value is WelcomeSignInStage.Verified

    // While the check runs the sheet can't be swiped or backed away (like iOS's interactiveDismissDisabled).
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || finishing.value || !confirming() },
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val reduceMotion = LocalWelcomeReduceMotion.current
    val signedIn by rememberUpdatedState(onSignedIn)

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    fun choose(persona: Persona) {
        if (stage.value != WelcomeSignInStage.Picker) return
        stage.value = WelcomeSignInStage.Verifying(persona)
    }

    // A short, calm hand-off moment, then the persona picker.
    LaunchedEffect(Unit) {
        if (stage.value != WelcomeSignInStage.Handoff || holdsStage) return@LaunchedEffect
        delay(1600)
        stage.value = WelcomeSignInStage.Picker
        if (autoSignIn) {
            delay(1000)
            choose(sample)
        }
    }

    // The check: the device's biometric/screen lock when it's set up, otherwise a short pulse. Either way the demo
    // carries on (no real account is checked), then "Verified" for 0.8 s and the sheet slides away.
    LaunchedEffect(stage.value) {
        when (val s = stage.value) {
            is WelcomeSignInStage.Verifying -> if (!holdsStage) {
                val passed = welcomeAuthenticate(context, s.persona)
                if (!passed) delay(700)
                stage.value = WelcomeSignInStage.Verified(s.persona)
            }
            is WelcomeSignInStage.Verified -> {
                DNHaptics.success(view)
                if (holdsStage) return@LaunchedEffect
                delay(800)
                finishing.value = true
                // Signs in even if the slide-away is cut short: the check has passed.
                try {
                    sheetState.hide()
                } finally {
                    signedIn(s.persona)
                }
            }
            else -> Unit
        }
    }

    val isConfirming = confirming()
    // A large sheet: it stops 12 dp below the status bar so the story still shows above it.
    val topGap = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp
    ModalBottomSheet(
        onDismissRequest = { if (!isConfirming || finishing.value) onDismiss() },
        sheetState = sheetState,
        containerColor = DNColor.surfaceSecondary,
        shape = RoundedCornerShape(topStart = DNRadius.sheet, topEnd = DNRadius.sheet),
        // material3 1.3 reads shouldDismissOnBackPress once, when the sheet opens (before any check), and its own
        // back callback then hides the sheet without asking confirmValueChange. So Back is handled here instead.
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        // Back closes the sheet like Cancel; while the check runs it's swallowed.
        BackHandler { if (!confirming()) close() }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val sheetHeight = if (constraints.hasBoundedHeight) (maxHeight - topGap).coerceAtLeast(320.dp) else 640.dp
            WelcomeDebugFontScale {
                WelcomeSignInContent(sheetHeight, isConfirming, reduceMotion, stage.value, ::close, ::choose)
            }
        }
    }
}

@Composable
private fun WelcomeSignInContent(
    height: Dp,
    isConfirming: Boolean,
    reduceMotion: Boolean,
    stage: WelcomeSignInStage,
    close: () -> Unit,
    choose: (Persona) -> Unit,
) {
    Column(Modifier.fillMaxWidth().height(height).navigationBarsPadding()) {
        // Close (the Android kit's sheet title row: action at the end). Hidden while the check runs.
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp)) {
            if (!isConfirming) {
                TonalIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Cancel",
                    onClick = { close() },
                    modifier = Modifier.align(Alignment.CenterEnd),
                    container = DNColor.surfacePrimary,
                )
            }
        }
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                if (reduceMotion) fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.base())
                else fadeIn(DNMotion.smooth()) togetherWith fadeOut(DNMotion.fast())
            },
            // Verifying → Verified stays the same view (no fade): only its tick and title change, so TalkBack's
            // live region reads "Verified, Signing in as …" like iOS's announcement.
            contentKey = {
                when (it) {
                    WelcomeSignInStage.Handoff -> "handoff"
                    WelcomeSignInStage.Picker -> "picker"
                    is WelcomeSignInStage.Verifying, is WelcomeSignInStage.Verified -> "check"
                }
            },
            modifier = Modifier.weight(1f),
            label = "welcome-sign-in",
        ) { s ->
            when (s) {
                WelcomeSignInStage.Handoff -> WelcomeHandoff(reduceMotion)
                WelcomeSignInStage.Picker -> WelcomePersonaPicker(choose)
                is WelcomeSignInStage.Verifying -> WelcomeVerify(s.persona, verified = false, reduceMotion)
                is WelcomeSignInStage.Verified -> WelcomeVerify(s.persona, verified = true, reduceMotion)
            }
        }
    }
}

// MARK: - Biometric check

private fun Context.welcomeFragmentActivity(): FragmentActivity? {
    var c: Context? = this
    while (c != null) {
        if (c is FragmentActivity) return c
        c = (c as? ContextWrapper)?.baseContext
    }
    return null
}

/**
 * Asks for the fingerprint/face or screen lock when the device has one set up (BIOMETRIC_STRONG | DEVICE_CREDENTIAL;
 * API 28–29 can't combine STRONG with the credential, so it asks for WEAK there). Returns false straight away when
 * nothing is enrolled, so the demo never blocks.
 */
private suspend fun welcomeAuthenticate(context: Context, persona: Persona): Boolean {
    val activity = context.welcomeFragmentActivity() ?: return false
    val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    val available = runCatching { BiometricManager.from(activity).canAuthenticate(authenticators) }.getOrNull()
    if (available != BiometricManager.BIOMETRIC_SUCCESS) return false
    return suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (cont.isActive) cont.resume(false)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sign in to DubaiNow")
            .setSubtitle("Signing in as ${persona.fullName}")
            .setAllowedAuthenticators(authenticators)
            .build()
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
        runCatching { prompt.authenticate(info) }.onFailure { if (cont.isActive) cont.resume(false) }
    }
}

// MARK: - Hand-off

/** DubaiNow → UAE PASS, with dots travelling between the two marks. */
@Composable
private fun WelcomeHandoff(reduceMotion: Boolean) {
    WelcomeSheetCentered(paneTitle = "Signing in with UAE PASS") {
        Row(
            Modifier.clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(72.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(DNColor.surfacePrimary),
                contentAlignment = Alignment.Center,
            ) { DNLogo(height = 34.dp) }
            WelcomeHandoffDots(reduceMotion)
            WelcomeUAEPassMark()
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Signing in with UAE PASS",
                style = DNTextStyle.Title2.style, color = DNColor.textPrimary, textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "We’re handing you over securely. You’ll come straight back here.",
                style = DNTextStyle.Body.style, color = DNColor.textSecondary, textAlign = TextAlign.Center,
            )
        }
        if (reduceMotion) CircularProgressIndicator(color = DNColor.actionAccent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun WelcomeHandoffDots(reduceMotion: Boolean) {
    val count = 5
    val head = if (reduceMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "handoff").animateFloat(
            initialValue = -1f, targetValue = count + 1f,
            animationSpec = infiniteRepeatable(tween(1100, easing = { it }), RepeatMode.Restart), label = "head",
        )
    }
    Row(Modifier.width(70.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Canvas(
                Modifier.size(6.dp).graphicsLayer {
                    val near = head?.let { maxOf(0f, 1f - abs(i - it.value) / 1.6f) } ?: 0f
                    scaleX = 1f + 0.35f * near
                    scaleY = 1f + 0.35f * near
                    alpha = if (head == null) 0.35f else 0.18f + 0.72f * near
                },
            ) { drawCircle(DNColor.textPrimary) }
        }
    }
}

/** The UAE PASS mark as used on the sign-in pill: black disc, fingerprint. */
@Composable
private fun WelcomeUAEPassMark() {
    Box(
        Modifier.size(72.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(Color.Black),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = DNColor.white, modifier = Modifier.size(38.dp)) }
}

/**
 * Centred sheet content with a little more space below than above (like iOS's two bottom spacers). [paneTitle] is
 * what TalkBack announces when the stage appears (the sheet swaps stages in place).
 */
@Composable
private fun WelcomeSheetCentered(paneTitle: String, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().semantics { this.paneTitle = paneTitle }) {
        val minHeight = maxHeight
        // Scrolls only when larger text needs it; otherwise the weights centre it.
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = minHeight).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
                content = content,
            )
            Spacer(Modifier.weight(2f))
        }
    }
}

// MARK: - Persona picker

@Composable
private fun WelcomePersonaPicker(choose: (Persona) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            // TalkBack announces the picker when it replaces the hand-off (like iOS's screen-changed post).
            .semantics { paneTitle = "Choose a demo resident" }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 24.dp),
    ) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // The UAE PASS badge is a mark (fixed 13 pt on iOS too): it keeps its size with larger text.
            WelcomeFontScaleCap(1f) {
                Row(
                    Modifier.height(26.dp).clip(CircleShape).background(Color.Black).padding(horizontal = 10.dp).clearAndSetSemantics {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = DNColor.white, modifier = Modifier.size(15.dp))
                    Text("UAE PASS", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.white, maxLines = 1)
                }
            }
            Text(
                "Choose a demo resident",
                style = DNTextStyle.Title1.style, color = DNColor.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text("Each one has their own bills, cars, family and requests.", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)) {
            val people = Personas.all
            people.forEachIndexed { i, persona ->
                WelcomePersonaRow(persona) { choose(persona) }
                if (i < people.lastIndex) {
                    Box(Modifier.fillMaxWidth().padding(start = 78.dp).height(0.5.dp).background(DNColor.listRowSeparator))
                }
            }
        }
        Row(
            Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(14.dp))
            Text("This is a demo. No real accounts are used.", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
    }
}

@Composable
private fun WelcomePersonaRow(persona: Persona, onClick: () -> Unit) {
    val large = LocalDensity.current.fontScale >= 1.5f
    val texts: @Composable ColumnScope.() -> Unit = {
        Text(persona.fullName, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.listRowTitle)
        // A persona without a tagline keeps its name centred on the avatar (no empty second line).
        if (persona.tagline.isNotBlank()) {
            Text(persona.tagline, style = DNTextStyle.Footnote.style, color = DNColor.listRowSupporting)
        }
    }
    // The initials are decoration: TalkBack reads the name.
    val avatar: @Composable () -> Unit = { WelcomeAvatar(persona, 48.dp) }
    val rowModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 72.dp)
        .clickable(role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) {}
        .padding(horizontal = 16.dp, vertical = 12.dp)
    if (large) {
        // Larger text: the avatar sits above the name so nothing truncates.
        Column(rowModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            avatar()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = texts)
        }
    } else {
        // No chevron (Paper LOA-0: Android navigation rows have none); the whole row is the target.
        Row(rowModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            avatar()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp), content = texts)
        }
    }
}

/**
 * A persona's avatar. Its initials sit in a fixed circle, so they keep their drawn size with larger text (the shared
 * Avatar scales them and a 26 dp one clips at 200%); TalkBack skips it and reads the name next to it.
 */
@Composable
private fun WelcomeAvatar(persona: Persona, size: Dp) {
    WelcomeFontScaleCap(1f) {
        Avatar(persona.initials, persona.avatarHue, Modifier.clearAndSetSemantics {}, size = size)
    }
}

// MARK: - The check

@Composable
private fun WelcomeVerify(persona: Persona, verified: Boolean, reduceMotion: Boolean) {
    val pulse = if (!verified && !reduceMotion) {
        rememberInfiniteTransition(label = "verify").animateFloat(
            initialValue = 1f, targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
        )
    } else {
        null
    }
    // The same view for both states, so the tick replaces the fingerprint in place and TalkBack's live region
    // reads "Verified, Signing in as …" when the check passes.
    val surface by animateColorAsState(
        if (verified) DNColor.statusSuccessSurface else DNColor.actionAccentSubtle,
        if (reduceMotion) DNMotion.base() else DNMotion.smooth(), label = "verify-surface",
    )
    WelcomeSheetCentered(paneTitle = "Confirming it’s you") {
        Box(
            Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(surface)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(verified, animationSpec = if (reduceMotion) DNMotion.base() else DNMotion.fast(), label = "verify-glyph") { done ->
                if (done) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = DNColor.statusSuccessForeground, modifier = Modifier.size(56.dp))
                } else {
                    Icon(
                        Icons.Rounded.Fingerprint, contentDescription = null, tint = DNColor.actionAccent,
                        modifier = Modifier.size(60.dp).graphicsLayer { alpha = pulse?.value ?: 1f },
                    )
                }
            }
        }
        Column(
            Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (verified) "Verified" else "Confirming it’s you",
                style = DNTextStyle.Title2.style, color = DNColor.textPrimary, textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                WelcomeAvatar(persona, 26.dp)
                Text(
                    "Signing in as ${persona.fullName}",
                    style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}
