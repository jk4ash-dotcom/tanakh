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

    fun forToken(token: Token, gloss: Gloss?): DisplayedGloss {
        if (token.divineName || isYhwhLemma(token)) {
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
        val primary = sanitizeLine(gloss.primary) ?: gloss.primary
        val senses = gloss.senses
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

    private fun isYhwhLemma(token: Token): Boolean =
        token.lemmaId == "H3068" || token.glossId == "H3068" ||
            token.he.contains("יהוה")

    private fun yhwhDisplay(gloss: Gloss?): DisplayedGloss {
        val primary = when {
            gloss?.primary.equals("LORD", ignoreCase = true) -> "LORD"
            gloss?.primary.equals("God", ignoreCase = true) -> "God"
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

    private fun sanitizeLine(line: String?): String? {
        if (line.isNullOrBlank()) return null
        if (FORBIDDEN.containsMatchIn(line)) return null
        return line.trim()
    }
}
