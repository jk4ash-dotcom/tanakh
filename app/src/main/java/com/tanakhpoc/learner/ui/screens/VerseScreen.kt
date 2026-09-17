package com.tanakhpoc.learner.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanakhpoc.learner.data.Gloss
import com.tanakhpoc.learner.data.Token
import com.tanakhpoc.learner.data.Verse
import com.tanakhpoc.learner.ui.theme.ChipHebrew
import com.tanakhpoc.learner.ui.theme.ChipPhonetic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseScreen(
    verse: Verse,
    resolveGloss: (String?) -> Gloss?,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf<Token?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val title = if (verse.book == "Gen") {
        "Genesis ${verse.chapter}:${verse.verse}"
    } else {
        "${verse.book} ${verse.chapter}:${verse.verse}"
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
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                verse.words.forEach { token ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.widthIn(min = 64.dp)
                    ) {
                        AssistChip(
                            onClick = { selected = token },
                            label = {
                                Text(
                                    token.he,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Serif,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        textDirection = TextDirection.Rtl
                                    )
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = ChipHebrew)
                        )
                        AssistChip(
                            onClick = { selected = token },
                            label = {
                                Text(token.phonetic, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = ChipPhonetic)
                        )
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
            Spacer(Modifier.height(12.dp))
            Text(
                "Tap a phonetic chip for possible sense(s). Gloss ≠ verse translation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val token = selected
    if (token != null) {
        val gloss = resolveGloss(token.glossId)
        ModalBottomSheet(onDismissRequest = { selected = null }, sheetState = sheetState) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Possible sense(s)", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("${token.he} · ${token.phonetic}", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
                if (token.divineName) {
                    Text(
                        "Divine name: יהוה / YHWH (no vocalization invented)",
                        style = MaterialTheme.typography.bodyMedium,
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
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                token.ketiv?.let {
                    Text("Ketiv (written): $it — phonetic follows qere", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                if (gloss == null) {
                    Text("No gloss available for this token.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text(gloss.primary, style = MaterialTheme.typography.titleMedium)
                    gloss.senses.filter { it.isNotBlank() && it != gloss.primary }.forEach { s ->
                        Text("• $s", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                    }
                    gloss.definition?.takeIf { it.isNotBlank() }?.let { def ->
                        Spacer(Modifier.height(8.dp))
                        Text(def.take(400), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Source: ${gloss.source.ifBlank { "lexicon" }}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                Text("Gloss ≠ verse translation", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
