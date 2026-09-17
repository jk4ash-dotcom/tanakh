package com.tanakhpoc.learner.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.tanakhpoc.learner.R

/** Embedded Noto Sans Hebrew (SIL OFL 1.1) for reliable Hebrew glyph coverage on device. */
val HebrewFontFamily = FontFamily(
    Font(R.font.noto_sans_hebrew_regular, FontWeight.Normal),
    Font(R.font.noto_sans_hebrew_medium, FontWeight.Medium)
)
