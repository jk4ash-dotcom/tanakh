package com.tanakhpoc.learner.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import com.tanakhpoc.learner.data.ConcordanceLink
import com.tanakhpoc.learner.data.ConcordanceRules
import com.tanakhpoc.learner.data.FocusPolicy
import androidx.compose.runtime.saveable.rememberSaveable
import com.tanakhpoc.learner.ui.theme.ConcordanceHighlight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanakhpoc.learner.data.BookTitles
import com.tanakhpoc.learner.ui.components.DssVariantBookBanner
import com.tanakhpoc.learner.ui.components.DssVariantBottomSheet
import com.tanakhpoc.learner.ui.components.DssVariantIndicator
import com.tanakhpoc.learner.ui.components.DssVariantSeamMarker
import com.tanakhpoc.learner.ui.components.DssVariantWordMarker
import com.tanakhpoc.learner.data.DssPlacement
import com.tanakhpoc.learner.data.DssResolvedNote
import com.tanakhpoc.learner.data.Gloss
import com.tanakhpoc.learner.data.GlossDisplay
import com.tanakhpoc.learner.data.Token
import com.tanakhpoc.learner.data.Verse
import com.tanakhpoc.learner.ui.theme.ChipHebrew
import com.tanakhpoc.learner.ui.theme.ChipPhonetic
import com.tanakhpoc.learner.ui.theme.HebrewFontFamily
import com.tanakhpoc.learner.ui.theme.Ink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseScreen(
    verse: Verse,
    resolveGloss: (String?) -> Gloss?,
    dssNotes: List<DssResolvedNote> = emptyList(),
    focusWordIndex: Int = -1,
    concordanceLink: (Token) -> ConcordanceLink? = { null },
    onSeeAll: (String) -> Unit = {},
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf<Token?>(null) }
    var dssOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val title = BookTitles.verseLabel(verse.book, verse.chapter, verse.verse)
    val tokens = GlossDisplay.displayTokens(verse)
    val visibleDss = dssNotes
    val bookBanners = visibleDss.filter { it.placement == DssPlacement.BOOK_BANNER }
    val seamMarkers = visibleDss.filter { it.placement == DssPlacement.SEAM_MARKER }
    val verseIndicators = visibleDss.filter { it.placement == DssPlacement.VERSE_INDICATOR }
    // Concordance jump: scroll the chip row to the token and flash it (by wordIndex).
    val chipScroll = rememberScrollState()
    val chipX = remember { mutableStateMapOf<Int, Int>() }
    val flash = remember { Animatable(0f) }
    // Critic L6/L7: only a real wordIndex may focus; flash once (consumed survives recreation/back).
    val focus = FocusPolicy.validFocus(verse, focusWordIndex)
    var flashConsumed by rememberSaveable(verse.id, focusWordIndex) { mutableStateOf(false) }
    LaunchedEffect(focus, chipX[focus ?: -1]) {
        val f = focus ?: return@LaunchedEffect
        val x = chipX[f] ?: return@LaunchedEffect
        if (flashConsumed) return@LaunchedEffect
        chipScroll.animateScrollTo((x - 48).coerceAtLeast(0))
        repeat(2) {
            flash.animateTo(1f, tween(280))
            flash.animateTo(0f, tween(420))
        }
        flashConsumed = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Hebrew + phonetic (OSHB order, LTR paired chips)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (bookBanners.isNotEmpty()) {
                DssVariantBookBanner(
                    notes = bookBanners,
                    onOpen = { dssOpen = true }
                )
            }
            if (seamMarkers.isNotEmpty()) {
                DssVariantSeamMarker(
                    notes = seamMarkers,
                    onOpen = { dssOpen = true }
                )
            }
            // Force LTR so RTL device locale cannot reverse OSHB token order.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(chipScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    tokens.forEachIndexed { index, token ->
                        val wordNotes = visibleDss.filter { note ->
                            if (note.placement != DssPlacement.WORD_CHIP) return@filter false
                            val start = note.anchor.wordIndex ?: return@filter false
                            val end = note.anchor.wordIndexEnd ?: start
                            index in start..end
                        }
                        val focused = focus != null && index == focus && !flashConsumed
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .widthIn(min = 72.dp)
                                .onGloballyPositioned { chipX[index] = it.positionInParent().x.toInt() }
                                .testTag("chip-$index")
                                .then(
                                    if (focused) Modifier.background(
                                        ConcordanceHighlight.copy(alpha = 0.25f + 0.6f * flash.value),
                                        RoundedCornerShape(10.dp)
                                    ) else Modifier
                                )
                        ) {
                            AssistChip(
                                onClick = { selected = token },
                                modifier = Modifier.heightIn(min = 40.dp),
                                label = {
                                    Text(
                                        text = token.he,
                                        color = Ink,
                                        fontSize = 20.sp,
                                        lineHeight = 28.sp,
                                        fontFamily = HebrewFontFamily,
                                        textAlign = TextAlign.Center,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible,
                                        maxLines = 1,
                                        style = TextStyle(
                                            textDirection = TextDirection.Rtl,
                                            fontFamily = HebrewFontFamily
                                        )
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = ChipHebrew,
                                    labelColor = Ink
                                )
                            )
                            AssistChip(
                                onClick = { selected = token },
                                label = {
                                    Text(
                                        token.phonetic,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Ink,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = ChipPhonetic,
                                    labelColor = Ink
                                )
                            )
                            if (wordNotes.isNotEmpty()) {
                                DssVariantWordMarker(
                                    notes = wordNotes,
                                    onOpen = { dssOpen = true }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("JPS 1917 (verse)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(verse.english.text, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${verse.english.source} · ${verse.english.license}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (verseIndicators.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                DssVariantIndicator(
                    notes = verseIndicators,
                    onOpen = { dssOpen = true }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Tap a phonetic chip for possible sense(s). Gloss ≠ verse translation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (dssOpen && visibleDss.isNotEmpty()) {
        DssVariantBottomSheet(
            notes = visibleDss,
            onDismiss = { dssOpen = false }
        )
    }

    val token = selected
    if (token != null) {
        val shown = GlossDisplay.forToken(token, resolveGloss(token.glossId))
        ModalBottomSheet(onDismissRequest = { selected = null }, sheetState = sheetState) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Possible sense(s)", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = token.he,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = HebrewFontFamily,
                        textDirection = TextDirection.Rtl
                    ),
                    fontFamily = HebrewFontFamily,
                    color = Ink
                )
                Text(
                    token.phonetic,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (token.divineName) {
                    Text(
                        "Divine name: יהוה / YHWH (no vocalization invented)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = HebrewFontFamily),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                token.lemmaId?.let {
                    Text(
                        "Lemma $it${token.morph?.let { m -> " · $m" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                token.procliticNote?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                token.ketiv?.let {
                    Text(
                        "Ketiv (written): $it — phonetic follows qere",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = HebrewFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(shown.primary, style = MaterialTheme.typography.titleMedium)
                shown.senses.forEach { s ->
                    Text("• $s", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                }
                shown.policyNote?.let { note ->
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                shown.definition?.let { def ->
                    Spacer(Modifier.height(8.dp))
                    Text(def, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (shown.source.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Source: ${shown.source}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                when (val link = concordanceLink(token)) {
                    is ConcordanceLink.Lemma -> TextButton(
                        onClick = { selected = null; onSeeAll(link.key) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "${ConcordanceRules.countLabel(link.count)} · See all",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    ConcordanceLink.FunctionWord -> Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            ConcordanceRules.FUNCTION_WORD,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    ConcordanceLink.Unavailable -> Unit
                    null -> Text(
                        "Loading concordance…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Gloss ≠ verse translation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
