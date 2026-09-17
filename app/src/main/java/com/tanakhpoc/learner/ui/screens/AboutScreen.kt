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
import com.tanakhpoc.learner.data.PackMeta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(meta: PackMeta, onBack: () -> Unit) {
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
            Text("Tanakh Learner (POC)", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Accuracy-first offline Hebrew Tanakh learner. Scope: ${meta.scope.ifBlank { "Genesis 1–3" }}.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Text("Hebrew text", style = MaterialTheme.typography.titleMedium)
            Text(
                "OSHB / morphhb WLC ${meta.hebrew["pin"] ?: "v.2.2"} — PD text; lemma/morphology CC BY 4.0 (Open Scriptures). Not UXLC.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("English (verse row)", style = MaterialTheme.typography.titleMedium)
            Text(
                "JPS 1917 (Public Domain). Verse-level only — not word-aligned. English may use God/Lord/LORD independently of YHWH policy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Phonetics (Sofer SBL-Learner)", style = MaterialTheme.typography.titleMedium)
            Text(
                meta.phonetics["policy"]
                    ?: "From OSHB niqqud via hebrew-transliteration (MIT) + Sofer SBL-Learner: Biblical/Tiberian, digraphs sh/kh/ts/ʾ/ʿ, vocal shewa ĕ, NOT Modern Israeli. יהוה → YHWH only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Glosses", style = MaterialTheme.typography.titleMedium)
            Text(
                "${meta.glosses["primary"] ?: "TBESH CC BY 4.0"}; fallback ${meta.glosses["fallback"] ?: "HebrewStrong.xml"}. UI: “Possible sense(s)” — Gloss ≠ verse translation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Display order", style = MaterialTheme.typography.titleMedium)
            Text(
                meta.display["tokenOrder"]
                    ?: "Single token array keeps OSHB order. LTR paired chips are display-only — do not naïve-reverse the verse string.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("Hebrew UI font", style = MaterialTheme.typography.titleMedium)
            Text(
                "Noto Sans Hebrew (Google / Noto Project) — SIL Open Font License 1.1. Embedded for reliable Hebrew glyph coverage on device chips and gloss sheet. See third_party/NotoSansHebrew/.",
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
            if (meta.gaps.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Gaps", style = MaterialTheme.typography.titleMedium)
                Text(meta.gaps.joinToString("\n") { "• $it" }, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "App ${BuildConfig.VERSION_NAME} · pack ${meta.version} · ${BuildConfig.APPLICATION_ID}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
