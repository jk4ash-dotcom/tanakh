package com.tanakhpoc.learner.ui.components

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.tanakhpoc.learner.data.DssResolvedNote
import com.tanakhpoc.learner.ui.theme.HebrewFontFamily
import com.tanakhpoc.learner.ui.theme.Ink

/**
 * Subtle verse-level DSS indicator (hidden when [notes] empty).
 * Does not clutter Hebrew chips.
 */
@Composable
fun DssVariantIndicator(
    notes: List<DssResolvedNote>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) return
    val label = notes.first().uxLabel.ifBlank { "Qumran reading" }
    TextButton(onClick = onOpen, modifier = modifier) {
        Text(
            "◇ $label",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
    }
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
