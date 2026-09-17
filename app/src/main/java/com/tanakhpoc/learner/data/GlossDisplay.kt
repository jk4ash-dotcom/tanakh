package com.tanakhpoc.learner.data

/**
 * UI-facing gloss presentation under Sofer divine-name policy.
 * Never surfaces Jehovah / ye.ho.vah / Adonai-vowel dumps as fact.
 */
data class DisplayedGloss(
    val primary: String,
    val senses: List<String>,
    val definition: String?,
    val source: String,
    val policyNote: String? = null
)

object GlossDisplay {
    /** Vocalization / Jehovah dumps that must never appear as fact in UI. */
    private val FORBIDDEN = Regex(
        """(?i)jehovah|ye\.?\s*ho\.?\s*vah|yehovah|""" +
            """vowel\s*pointings?\s+of|a\.?\s*do\.?\s*na(?:y|i)?|""" +
            """adonai[-\s]?vowel|unpronounced\s+except"""
    )

    private val JEHOVAH_WORD = Regex("""(?i)jehovah|yehovah|ye\.?\s*ho\.?\s*vah""")

    fun forToken(token: Token, gloss: Gloss?): DisplayedGloss {
        if (token.divineName || isYhwhLemma(token) || consonantsAreYhwh(token.he)) {
            return yhwhDisplay(gloss)
        }
        if (gloss == null) {
            return DisplayedGloss(
                primary = "No gloss available for this token.",
                senses = emptyList(),
                definition = null,
                source = ""
            )
        }
        // HIGH: never `sanitizeLine ?: primary` — that re-leaks Jehovah-nissi (H3071 / Exod.17.15)
        val primary = safePrimary(gloss.primary, gloss.senses)
        val senses = gloss.senses
            .mapNotNull { sanitizeLine(it) }
            .map { rewriteJehovah(it) }
            .mapNotNull { sanitizeLine(it) }
            .filter { it.isNotBlank() && !it.equals(primary, ignoreCase = true) }
            .distinct()
            .take(6)
        return DisplayedGloss(
            primary = primary,
            senses = senses,
            definition = sanitizeDefinition(gloss.definition),
            source = gloss.source.ifBlank { "lexicon" }
        )
    }

    fun sanitizeDefinition(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !FORBIDDEN.containsMatchIn(it) }
            .joinToString("\n")
            .trim()
        if (cleaned.isEmpty() || FORBIDDEN.containsMatchIn(cleaned)) return null
        return cleaned.take(400)
    }

    /** Pure helper: chip/display order is always words[] as packed (never reversed). */
    fun displayTokens(verse: Verse): List<Token> = verse.words

    private fun isYhwhLemma(token: Token): Boolean {
        val id = token.lemmaId ?: token.glossId ?: return false
        return id == "H3068" || id == "H3069" ||
            id.startsWith("H3068") || id.startsWith("H3069")
    }

    private fun consonantsAreYhwh(he: String): Boolean {
        val cons = he.filter { it in '\u05D0'..'\u05EA' }
        return cons == "יהוה" || cons.endsWith("יהוה")
    }

    private fun yhwhDisplay(gloss: Gloss?): DisplayedGloss {
        val primary = when {
            gloss?.primary.equals("LORD", ignoreCase = true) -> "LORD"
            gloss?.primary.equals("God", ignoreCase = true) -> "God"
            gloss?.primary?.contains("God", ignoreCase = true) == true -> "God"
            else -> "LORD"
        }
        return DisplayedGloss(
            primary = primary,
            senses = listOf("divine name — see About"),
            definition = null,
            source = gloss?.source?.ifBlank { "policy" } ?: "policy",
            policyNote = "יהוה / YHWH — no vocalization invented"
        )
    }

    /**
     * Safe primary: never fall back to a forbidden raw primary (H3071 Jehovah-nissi leak).
     */
    private fun safePrimary(primary: String, senses: List<String>): String {
        sanitizeLine(primary)?.let { return it }
        val rewritten = rewriteJehovah(primary)
        sanitizeLine(rewritten)?.let { return it }
        senses.asSequence()
            .map { rewriteJehovah(it) }
            .mapNotNull { sanitizeLine(it) }
            .firstOrNull()
            ?.let { return it }
        return "—"
    }

    private fun rewriteJehovah(line: String): String =
        JEHOVAH_WORD.replace(line, "LORD")

    private fun sanitizeLine(line: String?): String? {
        if (line.isNullOrBlank()) return null
        if (FORBIDDEN.containsMatchIn(line)) return null
        return line.trim()
    }
}
