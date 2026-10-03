package com.salehtl.dubainownext.features.activity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusWeak
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNListGroup
import com.salehtl.dubainownext.design.components.DNListRow
import com.salehtl.dubainownext.design.components.DNRowDivider
import com.salehtl.dubainownext.design.components.DNRowIcon
import com.salehtl.dubainownext.design.components.TonalIconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// MARK: - Scan or upload → advance the request → "Sent to GDRFA"

/** Which request is being scanned or uploaded for (both finish the same way). */
internal class ActivityCaptureState {
    var scan by mutableStateOf<String?>(null)
    var upload by mutableStateOf<String?>(null)
}

@Composable
internal fun rememberActivityCaptureState() = remember { ActivityCaptureState() }

/**
 * Hosts the fake passport scan (full screen, camera-like) and the "Upload file" chooser. Both end
 * with the request moving on, a success haptic and a toast that says where it went.
 */
@Composable
internal fun ActivityCaptureHost(state: ActivityCaptureState) {
    val store = LocalStore.current
    val toasts = LocalToasts.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    fun send(id: String, delayMs: Long) {
        val entity = store.request(id)?.entity ?: "government"
        scope.launch {
            delay(delayMs)
            store.advanceRequest(id)
            DNHaptics.success(view)
            toasts.show("Sent to $entity")
        }
    }
    state.scan?.let { id ->
        val request = store.request(id)
        if (request != null) {
            ActivityPassportCaptureDialog(
                request = request,
                onClose = { state.scan = null },
                onUse = { state.scan = null; send(id, 150) },
            )
        } else {
            LaunchedEffect(id) { state.scan = null }
        }
    }
    state.upload?.let { id ->
        ActivityUploadSheet(
            onDismiss = { state.upload = null },
            onPick = { state.upload = null; send(id, 350) },
        )
    }
}

// MARK: - Upload chooser (stands in for the system photo picker / file picker)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityUploadSheet(onDismiss: () -> Unit, onPick: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Slide the sheet away before the request moves on (instead of cutting it off).
    var picked by remember { mutableStateOf(false) }
    val pick: () -> Unit = {
        if (!picked) {
            picked = true
            scope.launch { sheetState.hide() }.invokeOnCompletion { onPick() }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DNColor.surfaceSecondary,
        shape = RoundedCornerShape(topStart = DNRadius.sheet, topEnd = DNRadius.sheet),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DNSpace.sheetPadding).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Upload your passport copy", style = DNTextStyle.Title3.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                Text(
                    "Choose the photo page of your passport. It needs to be sharp, with all four corners showing.",
                    style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
                )
            }
            DNListGroup {
                DNListRow(
                    "Passport scan.pdf", subtitle = "Files", onClick = pick, showsChevron = false,
                    leading = { DNRowIcon(Icons.Rounded.Description) },
                )
                DNRowDivider(inset = 56.dp)
                DNListRow(
                    "Passport photo page", subtitle = "Photos", onClick = pick, showsChevron = false,
                    leading = { DNRowIcon(Icons.Rounded.Image) },
                )
            }
        }
    }
}

// MARK: - The capture screen

@Composable
private fun ActivityPassportCaptureDialog(request: RequestItem, onClose: () -> Unit, onUse: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let { w ->
                w.setDimAmount(0f)
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
        ActivityPassportCapture(request, onClose, onUse)
    }
}

/**
 * A camera-like dark screen with a passport frame guide and a shutter. Nothing is captured: the
 * "feed" is a drawn passport page that comes into focus, like a real scanner would.
 */
@Composable
private fun ActivityPassportCapture(request: RequestItem, onClose: () -> Unit, onUse: () -> Unit) {
    val view = LocalView.current
    val reduceMotion = activityReduceMotion()
    val scope = rememberCoroutineScope()
    var sharp by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf(false) }
    val flash = remember { Animatable(0f) }

    fun shoot() {
        DNHaptics.light(view)
        sharp = true
        if (reduceMotion) { captured = true; return }
        scope.launch {
            flash.animateTo(0.85f, tween(80))
            captured = true
            flash.animateTo(0f, tween(300))
        }
    }

    LaunchedEffect(Unit) {
        // The "camera" settles and focuses, then says it's ready.
        delay(if (reduceMotion) 100 else 900)
        sharp = true
        val demo = ActivityDemo.value
        if ((demo == "shoot" || demo == "send") && ActivityDemo.once("capture")) {
            delay(900); shoot()
            if (demo == "send") { delay(1200); onUse() }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DNColor.ink900, DNColor.ink950)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Top bar
            Row(Modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TonalIconButton(
                    Icons.Rounded.Close, "Close", onClick = onClose,
                    tint = DNColor.white, container = DNColor.white.copy(alpha = 0.14f),
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Passport photo page", style = DNTextStyle.Headline.style, color = DNColor.textOnInverse, modifier = Modifier.semantics { heading() })
                    Text(request.title, style = DNTextStyle.Footnote.style, color = DNColor.widgetOnInverseSecondary, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(1f))
            ActivityViewfinder(sharp = sharp || captured, captured = captured)
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = captured,
                transitionSpec = { fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.base()) },
                label = "capture-controls",
            ) { done ->
                if (done) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage).padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DNButton("Use this photo", onClick = onUse)
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(CircleShape)
                                .clickable(role = Role.Button) { captured = false },
                            contentAlignment = Alignment.Center,
                        ) { Text("Retake", style = DNTextStyle.Button.style, color = DNColor.textOnInverse) }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().padding(bottom = 28.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(88.dp).clip(CircleShape).clickable(role = Role.Button) { shoot() }
                                .semantics { contentDescription = "Take photo" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(78.dp).border(4.dp, DNColor.white, CircleShape))
                            Box(Modifier.size(64.dp).clip(CircleShape).background(DNColor.white))
                        }
                    }
                }
            }
        }
        if (flash.value > 0f) Box(Modifier.fillMaxSize().background(DNColor.white.copy(alpha = flash.value)))
    }
}

@Composable
private fun ActivityViewfinder(sharp: Boolean, captured: Boolean) {
    val blur by animateDpAsState(if (sharp) 0.dp else 5.dp, tween(500), label = "focus-blur")
    val zoom by animateFloatAsState(if (sharp) 1f else 1.04f, tween(500), label = "focus-zoom")
    val frame by animateColorAsState(if (sharp) DNColor.green300 else DNColor.white, tween(300), label = "frame")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            Modifier.padding(horizontal = 24.dp).fillMaxWidth().aspectRatio(1.42f)
                .clearAndSetSemantics {
                    contentDescription = if (captured) "Photo of your passport page" else "Camera viewfinder with a frame for your passport page"
                },
        ) {
            ActivityPassportPage(Modifier.fillMaxSize().padding(18.dp).scale(zoom).blur(blur))
            Canvas(Modifier.fillMaxSize()) {
                val l = minOf(size.width, size.height) * 0.16f
                val r = 14.dp.toPx()
                val w = size.width; val h = size.height
                val p = Path().apply {
                    moveTo(0f, l); lineTo(0f, r); quadraticTo(0f, 0f, r, 0f); lineTo(l, 0f)
                    moveTo(w - l, 0f); lineTo(w - r, 0f); quadraticTo(w, 0f, w, r); lineTo(w, l)
                    moveTo(w, h - l); lineTo(w, h - r); quadraticTo(w, h, w - r, h); lineTo(w - l, h)
                    moveTo(l, h); lineTo(r, h); quadraticTo(0f, h, 0f, h - r); lineTo(0f, h - l)
                }
                drawPath(p, frame, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Row(
            Modifier.heightIn(min = 36.dp).clip(CircleShape).background(DNColor.white.copy(alpha = 0.12f)).padding(horizontal = 14.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (sharp) Icons.Rounded.CheckCircle else Icons.Rounded.CenterFocusWeak, contentDescription = null,
                tint = if (sharp) DNColor.green300 else DNColor.white, modifier = Modifier.size(20.dp),
            )
            Crossfade(
                targetState = when {
                    captured -> "Sharp, and all four corners show"
                    sharp -> "Looks sharp · take the photo"
                    else -> "Hold steady inside the frame"
                },
                animationSpec = DNMotion.base(), label = "capture-status",
            ) { text ->
                Text(text, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textOnInverse)
            }
        }
    }
}

/** A drawn passport photo page (no real data: the persona's name and placeholder lines). */
@Composable
private fun ActivityPassportPage(modifier: Modifier = Modifier) {
    val persona = LocalStore.current.persona
    val last = (persona?.lastName ?: "RESIDENT").uppercase().replace(" ", "<")
    val first = (persona?.firstName ?: "DEMO").uppercase().replace(" ", "<")
    val country = if (persona?.id == "sara") "LBN" else "UTO"
    val mrz1 = "P<$country$last<<$first".padEnd(44, '<')
    val mrz2 = "RL<<<<821<5${country}9003147F3106307".padEnd(44, '<')
    BoxWithConstraints(
        modifier.shadow(18.dp, RoundedCornerShape(10.dp), ambientColor = DNColor.ink950, spotColor = DNColor.ink950)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(DNColor.huePeachStart, DNColor.hueLilacStart), start = Offset.Zero, end = Offset.Infinite)),
    ) {
        val w = maxWidth; val h = maxHeight
        Row(Modifier.padding(start = w * 0.05f, top = h * 0.08f), horizontalArrangement = Arrangement.spacedBy(w * 0.05f)) {
            Box(
                Modifier.width(w * 0.28f).height(h * 0.56f).clip(RoundedCornerShape(6.dp)).background(DNColor.hueLilacEnd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = DNColor.hueLilacInk.copy(alpha = 0.55f), modifier = Modifier.fillMaxSize().padding(w * 0.03f))
            }
            Column(Modifier.padding(top = h * 0.03f), verticalArrangement = Arrangement.spacedBy(h * 0.045f)) {
                listOf(0.7f, 0.9f, 0.55f, 0.8f, 0.6f, 0.75f).forEach { f ->
                    Box(Modifier.width(w * 0.55f * f).height(h * 0.035f).clip(CircleShape).background(DNColor.hueLilacInk.copy(alpha = 0.22f)))
                }
            }
        }
        val mrzSize = with(LocalDensity.current) { (w * 0.029f).toSp() }
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = w * 0.05f).padding(bottom = h * 0.07f).offset(y = 0.dp)) {
            listOf(mrz1, mrz2).forEach { line ->
                Text(
                    line, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = mrzSize,
                    color = DNColor.hueLilacInk.copy(alpha = 0.7f), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
