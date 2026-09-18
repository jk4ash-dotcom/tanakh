package com.tanakhpoc.learner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tanakhpoc.learner.data.CatalogBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    books: List<CatalogBook>,
    scope: String,
    onOpenBook: (String) -> Unit,
    onAbout: () -> Unit
) {
    val divisions = listOf("Torah", "Nevi'im", "Ketuvim")
    val grouped = divisions.mapNotNull { div ->
        val subset = books.filter { it.division == div }
        if (subset.isEmpty()) null else div to subset
    }.ifEmpty {
        listOf("Books" to books)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tanakh Learner") },
                actions = {
                    IconButton(onClick = onAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = "About")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    scope.ifBlank { "Full Tanakh (offline)" },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Jewish Tanakh order · OSHB Hebrew (v.2.2) · Sofer SBL-Learner phonetics · " +
                        "JPS 1917 English · TBESH glosses. Per-book packs load off the main thread.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            grouped.forEach { (division, divBooks) ->
                item(key = "hdr-$division") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        division,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        when (division) {
                            "Torah" -> "Genesis–Deuteronomy"
                            "Nevi'im" -> "Former + Latter Prophets (incl. Twelve)"
                            "Ketuvim" -> "Writings"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(divBooks, key = { it.osis }) { book ->
                    Card(
                        onClick = { onOpenBook(book.osis) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(book.title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${book.chapters} chapters · ${book.verses} verses" +
                                    if (book.aramaic) " · Aramaic sections flagged" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
