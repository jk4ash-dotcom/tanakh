package com.tanakhpoc.learner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanakhpoc.learner.data.DssResolvedNote
import com.tanakhpoc.learner.ui.theme.HebrewFontFamily
import com.tanakhpoc.learner.ui.theme.Ink

/**
 * Clear readable **Q** (Qumran) verse-level indicator (hidden when [notes] empty).
 * Does not clutter Hebrew chips; OSHB base text unchanged.
 */
@Composable
fun DssVariantIndicator(
    notes: List<DssResolvedNote>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) return
    val label = notes.first().uxLabel.ifBlank { "Qumran reading" }
    TextButton(
        onClick = onOpen,
        modifier = modifier.semantics { contentDescription = "Q — $label" }
    ) {
        Text(
            text = "Q",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "  $label",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/** Compact, readable **Q** marker below a Hebrew/phonetic word chip (not inside the chip). */
@Composable
fun DssVariantWordMarker(
    notes: List<DssResolvedNote>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) return
    TextButton(
        onClick = onOpen,
        modifier = modifier.semantics { contentDescription = "Q — Qumran reading" },
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Q",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/** Marker for a Hebrew/Aramaic language seam in the verse chip row. */
@Composable
fun DssVariantSeamMarker(
    notes: List<DssResolvedNote>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    DssVariantIndicator(notes = notes, onOpen = onOpen, modifier = modifier)
}

/** Banner for literary notes that target an entire book. */
@Composable
fun DssVariantBookBanner(
    notes: List<DssResolvedNote>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    DssVariantIndicator(notes = notes, onOpen = onOpen, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DssVariantBottomSheet(
    notes: List<DssResolvedNote>,
    onDismiss: () -> Unit
) {
    if (notes.isEmpty()) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            notes.forEachIndexed { index, note ->
                if (index > 0) Spacer(Modifier.height(20.dp))
                Text(note.uxLabel, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (note.dssHebrew.isNotBlank()) {
                    Text(
                        text = note.dssHebrew,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = HebrewFontFamily,
                            textDirection = TextDirection.Rtl
                        ),
                        fontFamily = HebrewFontFamily,
                        color = Ink
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (note.mss.isNotEmpty()) {
                    Text(
                        note.mss.joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(note.dssSummary, style = MaterialTheme.typography.bodyLarge)
                if (note.mtSummary.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        note.mtSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    note.disclaimer,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (note.refs.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        note.refs.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
