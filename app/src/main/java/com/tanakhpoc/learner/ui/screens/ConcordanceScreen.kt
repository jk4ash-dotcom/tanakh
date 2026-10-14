package com.tanakhpoc.learner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanakhpoc.learner.data.ConcordanceDisplay
import com.tanakhpoc.learner.data.ConcordanceEntry
import com.tanakhpoc.learner.data.Gloss
import com.tanakhpoc.learner.data.RenderedRow
import com.tanakhpoc.learner.data.TrimmedLine
import com.tanakhpoc.learner.ui.theme.ConcordanceHighlight
import com.tanakhpoc.learner.ui.theme.HebrewFontFamily
import com.tanakhpoc.learner.ui.theme.Ink

const val CONCORDANCE_PAGE = 30

/** A loaded page row: rendered strings + jump target. */
data class ConcordanceRowUi(val verseId: String, val wordIndex: Int, val rendered: RenderedRow)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcordanceScreen(
    entry: ConcordanceEntry,
    gloss: Gloss?,
    loadPage: suspend (page: Int) -> List<ConcordanceRowUi>,
    onOpenRow: (verseId: String, wordIndex: Int) -> Unit,
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    initialPages: Map<Int, List<ConcordanceRowUi>> = emptyMap()
) {
    val header = remember(entry.key) { ConcordanceDisplay.header(entry, gloss) }
    // Pages load lazily as rows scroll in; DN (6.5k rows) never loads up front.
    val pages = remember(entry.key) { mutableStateMapOf<Int, List<ConcordanceRowUi>>().apply { putAll(initialPages) } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Concordance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).testTag("concordance-list")
        ) {
            item(key = "header") { ConcordanceHeader(header, entry.isAramaic) }
            items(count = entry.count, key = { it }) { i ->
                val page = i / CONCORDANCE_PAGE
                val row = pages[page]?.getOrNull(i % CONCORDANCE_PAGE)
                if (row == null) {
                    LaunchedEffect(page) {
                        if (pages[page] == null) pages[page] = loadPage(page)
                    }
                    Text(
                        "…",
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(64.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ConcordanceRowView(row, onOpen = { onOpenRow(row.verseId, row.wordIndex) })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun ConcordanceHeader(h: List<String>, aramaic: Boolean) {
    // h = [headword, id, gloss, "Appears N times", "in this text (OSHB)"]
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                h[0],
                fontFamily = HebrewFontFamily,
                fontSize = 30.sp,
                color = Ink,
                style = TextStyle(textDirection = TextDirection.Rtl)
            )
            Spacer(Modifier.padding(horizontal = 6.dp))
            Column {
                Text(h[1], style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(h[2], style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("${h[3]} · ${h[4]}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            "Other forms of the same dictionary word, in Tanakh order. English is the whole JPS verse, not a word gloss.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    HorizontalDivider()
}

@Composable
private fun Badge(text: String, onClick: (() -> Unit)? = null) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(6.dp),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun hebrewAnnotated(line: TrimmedLine, highlight: Color) = buildAnnotatedString {
    if (line.trimmedStart) append("… ")
    line.words.forEachIndexed { i, w ->
        if (w.highlighted) {
            withStyle(SpanStyle(background = highlight, fontWeight = FontWeight.Bold)) { append(w.text) }
        } else append(w.text)
        if (i < line.words.lastIndex && !w.joinNext) append(' ')
    }
    if (line.trimmedEnd) append(" …")
}

@Composable
private fun ConcordanceRowView(row: ConcordanceRowUi, onOpen: () -> Unit) {
    var showKetiv by rememberSaveable(row.verseId, row.wordIndex) { mutableStateOf(false) }
    val r = row.rendered
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(r.ref, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(r.phonetic, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            r.badges.forEach { b ->
                Badge(b, onClick = if (b == "qere") ({ showKetiv = !showKetiv }) else null)
            }
        }
        if (showKetiv && r.ketivOnTap != null) {
            Text(
                r.ketivOnTap,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = HebrewFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            hebrewAnnotated(r.hebrew, ConcordanceHighlight),
            fontFamily = HebrewFontFamily,
            fontSize = 20.sp,
            lineHeight = 30.sp,
            color = Ink,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Start)
        )
        Text(
            r.english,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
