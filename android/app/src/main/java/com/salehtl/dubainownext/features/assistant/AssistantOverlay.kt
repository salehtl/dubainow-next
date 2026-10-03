package com.salehtl.dubainownext.features.assistant

import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.components.Toast
import com.salehtl.dubainownext.features.activity.ActivityCaptureHost
import com.salehtl.dubainownext.features.activity.rememberActivityCaptureState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import kotlin.coroutines.resume

/**
 * DubaiNow's own assistant, "like Siri": it covers the whole screen, but you never leave the
 * screen you're on (it stays visible and live behind a soft veil). The logo is its face, top
 * centre, in a white tonal orb; one white card hangs from it; your side (suggestions, the ask
 * bar, the one confirm) is at the bottom, under your thumb. Android: no Liquid Glass — a scrim
 * and an animated blue edge light. Paper C1 row 11 (V83-0, W1D-0, RUW-0 … UXH-0). Scripted.
 *
 * Shown by RootScreen while `router.isAssistantPresented`. `router.assistantSeed`:
 * "__listen__" = opened by touch and hold; any other text = a question to run straight away.
 * System back closes it.
 */
@Composable
fun AssistantOverlay() {
    val store = LocalStore.current
    val router = LocalRouter.current
    val toasts = LocalToasts.current
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberAssistantReduceMotion()

    val engine = remember { AssistantEngine(store, router, scope) }
    engine.view = view
    engine.authenticate = { title, reason -> assistantAuthenticate(context.assistantActivity(), title, reason) }
    engine.announce = { text -> if (text.isNotEmpty()) assistantAnnounce(view, text) }
    engine.showNote = { note ->
        val id = note.receiptID
        toasts.show(Toast(note.text, Icons.Rounded.CheckCircle, if (id != null) "Receipt" else null, id?.let { { router.present(SheetRoute.Receipt(it)) } }))
    }

    // "Start scanning": the tracker's own fake camera (Activity) opens over the assistant. Once the
    // photo is sent (the request moves on), the assistant closes on the updated screen.
    val capture = rememberActivityCaptureState()
    var scanning by remember { mutableStateOf<String?>(null) }
    engine.scanPassport = { id -> scanning = id; capture.scan = id }
    LaunchedEffect(scanning) {
        val id = scanning ?: return@LaunchedEffect
        val step = store.request(id)?.currentStepIndex
        snapshotFlow { store.request(id)?.currentStepIndex }.first { it != step }
        engine.close()
    }

    var awake by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        awake = true
        engine.start(router.assistantSeed)
    }
    LaunchedEffect(engine.phase) {
        if (engine.phase != AssistantPhase.Idle && engine.phase != AssistantPhase.Card) focusManager.clearFocus()
    }
    BackHandler(enabled = router.isAssistantPresented) { engine.close() }

    // The edge light calms when it's your turn and goes out when it's quiet.
    val edge by animateFloatAsState(
        when (engine.face) {
            AssistantFaceState.YourTurn -> 0.55f
            AssistantFaceState.Quiet, AssistantFaceState.Resting -> 0f
            else -> 1f
        },
        tween(300), label = "edge",
    )
    val barIn by animateFloatAsState(if (awake) 1f else 0f, DNMotion.bouncy(), label = "bar")

    // This full-screen semantics node also makes Compose drop the covered screen from the
    // accessibility tree, so TalkBack stays in the assistant (iOS: .isModal). Keep it.
    BoxWithConstraints(Modifier.fillMaxSize().semantics { paneTitle = "DubaiNow assistant" }) {
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // The face sits exactly where the header logo is (status bar + 8 + 28).
        val faceCenter = statusTop + 36.dp
        val cardTop = faceCenter + 45.dp
        var barHeight by remember { mutableStateOf(0.dp) }
        // The bar sits above the gesture bar, or above the keyboard while you type.
        val navBottom = maxOf(
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            WindowInsets.ime.asPaddingValues().calculateBottomPadding(),
        )

        AssistantBackdrop(edge, reduceMotion, bottomBand = barHeight, onTap = { if (fieldFocused) focusManager.clearFocus() else engine.close() })

        // One card at a time; a new answer replaces it and drops down out of the face.
        val cards = remember { mutableStateMapOf<String, AssistantCard>() }
        engine.card?.let { cards[it.id] = it }
        AnimatedContent(
            targetState = engine.card?.id,
            transitionSpec = {
                (fadeIn(DNMotion.base()) + scaleIn(DNMotion.smooth(), initialScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 0f)) +
                    slideInVertically(DNMotion.smooth()) { -it / 12 }) togetherWith fadeOut(DNMotion.fast())
            },
            modifier = Modifier.fillMaxWidth().padding(top = cardTop, start = 16.dp, end = 16.dp),
            label = "card",
        ) { id ->
            val card = id?.let { cards[it] }
            if (card != null) {
                AssistantCardView(
                    card = card,
                    english = engine.showsEnglish,
                    maxHeight = (maxHeight - cardTop - barHeight - 12.dp).coerceAtLeast(160.dp),
                    reduceMotion = reduceMotion,
                    onToggleEnglish = { DNHaptics.selection(view); engine.showsEnglish = !engine.showsEnglish },
                    onStop = { engine.stop() },
                    onTakeOver = { engine.takeOver() },
                    onAction = { engine.perform(it) },
                    onReceipt = { router.present(SheetRoute.Receipt(it)) },
                    onRate = { face, receiptID -> engine.rate(face, receiptID) },
                )
            } else Box(Modifier.fillMaxWidth())
        }
        LaunchedEffect(engine.card?.id) {
            delay(800) // after the old card has faded out
            cards.keys.retainAll(setOfNotNull(engine.card?.id))
        }

        AssistantBottomBar(
            engine = engine,
            suggestions = engine.suggestions(),
            reduceMotion = reduceMotion,
            focusRequester = focusRequester,
            onFieldFocus = { fieldFocused = it },
            onDismissKeyboard = { focusManager.clearFocus() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .onSizeChanged { barHeight = with(density) { it.height.toDp() } + 12.dp + navBottom }
                .graphicsLayer {
                    alpha = barIn.coerceIn(0f, 1f)
                    translationY = if (reduceMotion) 0f else (1f - barIn) * 40.dp.toPx()
                },
        )

        AssistantFace(
            state = engine.face, progress = engine.progress, awake = awake, reduceMotion = reduceMotion,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = faceCenter - 80.dp),
        )
    }

    ActivityCaptureHost(capture)
}

/**
 * TalkBack reads the new headline / "Your turn. Pay AED 1,020." (iOS posts announcements). Each answer
 * is a new card node, so a live region on the headline wouldn't be read when a card replaces another.
 */
@Suppress("DEPRECATION")
private fun assistantAnnounce(view: View, text: String) = view.announceForAccessibility(text)

/** "Remove animations" (animator duration scale 0) = Reduce Motion. */
@Composable
private fun rememberAssistantReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

private tailrec fun Context.assistantActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.assistantActivity()
    else -> null
}

/**
 * Fingerprint (or face) via androidx.biometric when the device has biometrics enrolled; otherwise
 * a short 0.9 s "Confirming…" so the demo never blocks. Returns false if the person cancels.
 */
private suspend fun assistantAuthenticate(activity: FragmentActivity?, title: String, reason: String): Boolean {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
    if (activity != null && BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
        return suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (cont.isActive) cont.resume(false)
                }
            })
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(reason)
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(authenticators)
                .build()
            prompt.authenticate(info)
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
    delay(900)
    return true
}
