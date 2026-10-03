package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.RowTrailing
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * All services, A to Z: every one of the catalogue's records in a plain list with sticky
 * letter sections and a letter rail, a type filter (M3 segmented button), and its own search field.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllServicesScreen() {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(ServicesData.Filter.parse(LaunchOptions.extras["servicesFilter"]) ?: ServicesData.Filter.All) }
    var query by rememberSaveable { mutableStateOf(LaunchOptions.extras["servicesQuery"] ?: "") }

    val base = ServicesData.aToZ(filter)
    val list = remember(filter, query) { base.filter { ServicesData.matches(it, query) } }
    val sections = remember(list) { ServicesData.letterSections(list) }
    val state = rememberLazyListState()

    // Lazy indices: filter (0), summary (1), then each letter header followed by its rows.
    val headerIndex = remember(sections) {
        var i = 2
        sections.associate { s -> (s.letter to i).also { i += 1 + s.services.size } }
    }
    ServicesLaunchScroll(state) { key -> headerIndex[key.uppercase()] }
    // Back to the top when the filter changes, not when the screen comes back from a pushed page.
    LaunchedEffect(Unit) { snapshotFlow { filter }.drop(1).collect { if (state.firstVisibleItemIndex > 1) state.scrollToItem(0) } }

    val showsRail = query.isBlank() && sections.size > 6

    Box(Modifier.fillMaxSize()) {
        ServicesPage(
            base = DNColor.surfacePrimary, state = state,
            above = {
                Column(ServicesInset.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("All services, A to Z", style = DNTextStyle.Title2.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                    ServicesSearchField(query, { query = it }, placeholder = "Search ${ServicesText.count(base.size, "service")}")
                }
            },
        ) {
            item(key = "filter") {
                val all = ServicesData.Filter.entries
                val pick = { f: ServicesData.Filter -> if (filter != f) { DNHaptics.selection(view); filter = f } }
                val labelStyle = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.SemiBold)
                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                BoxWithConstraints(ServicesInset.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
                    // Equal M3 segments keep 12 dp either side of their label. When a label can't fit on one line
                    // there ("Start to finish" on most phones, every label at large text sizes), wrapping M3 filter chips can.
                    val room = constraints.maxWidth / all.size - with(density) { 24.dp.roundToPx() }
                    val fits = remember(room, measurer) { all.all { measurer.measure(it.label, labelStyle).size.width <= room } }
                    if (!fits) {
                        ServicesFlow(Modifier.fillMaxWidth()) {
                            all.forEach { f -> ServicesChoiceChip(f.label, filter == f) { pick(f) } }
                        }
                    } else SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        all.forEachIndexed { i, f ->
                            SegmentedButton(
                                selected = filter == f,
                                onClick = { pick(f) },
                                shape = SegmentedButtonDefaults.itemShape(i, all.size),
                                colors = servicesSegmentedColors(),
                                icon = {},
                            ) {
                                Text(f.label, style = labelStyle, maxLines = 1)
                            }
                        }
                    }
                }
            }
            item(key = "summary") {
                Text(
                    servicesAToZSummary(filter, list.size), style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                    modifier = ServicesInset.padding(bottom = 8.dp),
                )
            }
            if (list.isEmpty()) item(key = "empty") { ServicesNoResults(query) }
            sections.forEach { section ->
                stickyHeader(key = "letter-${section.letter}") {
                    Text(
                        section.letter, style = DNTextStyle.Headline.style, color = DNColor.textPrimary,
                        modifier = Modifier.fillMaxWidth().background(DNColor.surfacePrimary)
                            .padding(horizontal = DNSpace.insetPage, vertical = 6.dp)
                            .semantics { heading(); contentDescription = "Letter ${section.letter}" },
                    )
                }
                section.services.forEachIndexed { i, s ->
                    item(key = s.id) {
                        Column(Modifier.padding(end = if (showsRail) 20.dp else 0.dp)) {
                            ServicesRow(s, trailing = RowTrailing.Chevron)
                            if (i < section.services.lastIndex) ServicesDivider(68.dp)
                        }
                    }
                }
            }
        }
        if (showsRail) {
            ServicesLetterRail(
                letters = sections.map { it.letter },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp, top = 140.dp),
            ) { letter ->
                headerIndex[letter]?.let { DNHaptics.selection(view); scope.launch { state.scrollToItem(it) } }
            }
        }
    }
}

private fun servicesAToZSummary(filter: ServicesData.Filter, count: Int): String = when (filter) {
    ServicesData.Filter.All -> "${ServicesText.count(count, "service")}: standalone services, bundles and journeys."
    ServicesData.Filter.Standalone -> "${ServicesText.count(count, "standalone service")}. One task with one entity."
    ServicesData.Filter.Bundle -> "${ServicesText.count(count, "bundle")}. The same job across many accounts, done in one go."
    ServicesData.Filter.Journey -> "${ServicesText.count(count, "journey")}. Life events that need several services, in order."
}

/**
 * The section index (Android has no native one): tap or drag along the rail to jump to a letter.
 * TalkBack users move by the letter headings instead, so the rail is hidden from it.
 */
@Composable
private fun ServicesLetterRail(letters: List<String>, modifier: Modifier = Modifier, onLetter: (String) -> Unit) {
    var last by remember { mutableIntStateOf(-1) }
    var height by remember { mutableIntStateOf(1) }
    fun pick(y: Float) {
        val i = ((y / height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        if (i != last) { last = i; onLetter(letters[i]) }
    }
    Column(
        modifier.width(20.dp)
            .onSizeChanged { height = maxOf(it.height, 1) }
            .pointerInput(letters) { detectTapGestures { pick(it.y); last = -1 } }
            .pointerInput(letters) { detectVerticalDragGestures(onDragEnd = { last = -1 }) { change, _ -> pick(change.position.y) } }
            .clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { Text(it, style = DNTextStyle.Caption2.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink) }
    }
}
