package com.tanakhpoc.learner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tanakhpoc.learner.BuildConfig
import com.tanakhpoc.learner.data.CatalogTotals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    version: String,
    scope: String,
    totals: CatalogTotals,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
                .padding(24.dp)
        ) {
            Text("Tanakh Learner", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Accuracy-first offline Hebrew Tanakh learner. Scope: ${scope.ifBlank { "Torah" }}. " +
                    "${totals.books} books · ${totals.verses} verses · ${totals.glosses} glosses · " +
                    "${totals.ketivQere} ketiv/qere.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Text("Navigation", style = MaterialTheme.typography.titleMedium)
            Text(
                "Jewish Tanakh order (Torah → Nevi’im → Ketuvim). Daniel is in Writings when present — not Christian/filename order.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Hebrew text", style = MaterialTheme.typography.titleMedium)
            Text(
                "OSHB / morphhb WLC v.2.2 — PD text; lemma/morphology CC BY 4.0 (Open Scriptures). Not UXLC. Surfaces are not NFC-normalized.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("English (verse row)", style = MaterialTheme.typography.titleMedium)
            Text(
                "JPS 1917 (Public Domain). Verse-level only — not word-aligned. Hebrew WLC verse IDs are primary; engjps mapped via OSHB VerseMap.xml where WLC≠KJV.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Phonetics (Sofer SBL-Learner)", style = MaterialTheme.typography.titleMedium)
            Text(
                "From OSHB niqqud via hebrew-transliteration (MIT) + Sofer SBL-Learner: Biblical/Tiberian, digraphs sh/kh/ts/ʾ/ʿ, vocal shewa ĕ, NOT Modern Israeli. יהוה → YHWH only. Biblical Aramaic (Dan/Ezra) must not silently use Hebrew-only rules — flagged for Sofer-approved handling.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Glosses", style = MaterialTheme.typography.titleMedium)
            Text(
                "TBESH CC BY 4.0 (STEPBible); fallback HebrewStrong.xml. UI: “Possible sense(s)” — Gloss ≠ verse translation. Jehovah / ye.ho.vah dumps sanitized.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Display order", style = MaterialTheme.typography.titleMedium)
            Text(
                "Single token array keeps OSHB order. LTR paired chips are display-only — do not naïve-reverse the verse string. Noto Sans Hebrew for chips.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Licenses", style = MaterialTheme.typography.titleMedium)
            Text(
                "OSHB (PD + CC BY 4.0) · TBESH (CC BY 4.0, STEPBible) · JPS 1917 (PD) · hebrew-transliteration (MIT) · Noto Sans Hebrew (SIL OFL 1.1).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "App ${BuildConfig.VERSION_NAME} · pack $version · ${BuildConfig.APPLICATION_ID}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
