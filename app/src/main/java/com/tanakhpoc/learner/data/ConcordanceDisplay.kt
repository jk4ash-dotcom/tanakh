package com.tanakhpoc.learner.data

/** One rendered Hebrew word in a concordance row (pure, JVM-testable). */
data class RowWord(val wordIndex: Int, val text: String, val highlighted: Boolean, val joinNext: Boolean)

data class TrimmedLine(val words: List<RowWord>, val trimmedStart: Boolean, val trimmedEnd: Boolean) {
    /** Plain RTL text (logical order) — used by tests / G4 grep. */
    fun plain(): String = buildString {
        if (trimmedStart) append("… ")
        words.forEachIndexed { i, w ->
            append(w.text)
            if (i < words.lastIndex && !w.joinNext) append(' ')
        }
        if (trimmedEnd) append(" …")
    }
}

/** Every string a concordance row/header puts on screen (G4 snapshot grep). */
data class RenderedRow(
    val ref: String,
    val hebrew: TrimmedLine,
    val english: String,
    val phonetic: String,
    val badges: List<String>,
    val ketivOnTap: String?
) {
    fun allStrings(): List<String> = listOf(ref, hebrew.plain(), english, phonetic) + badges + listOfNotNull(ketivOnTap)
}

object ConcordanceDisplay {
    const val MAQAF = '\u05BE'
    const val MAX_WORDS = 16


    /** Divine-name sanitizer for the Hebrew token: never show a vocalized YHWH. */
    fun displayHe(token: Token): String {
        val cons = token.he.filter { it in '\u05D0'..'\u05EA' }
        return if (token.divineName || cons == "יהוה" || cons.endsWith("יהוה") ||
            token.lemmaId?.startsWith("H3068") == true || token.lemmaId?.startsWith("H3069") == true
        ) {
            token.he.filter { it !in '\u0591'..'\u05C7' || it == MAQAF }
        } else token.he
    }


    private fun joinsNext(t: Token) = t.he.endsWith(MAQAF)

    /** Maqaf groups as index ranges (a group never splits). */
    fun groups(words: List<Token>): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = 0
        for (i in words.indices) {
            if (!joinsNext(words[i]) || i == words.lastIndex) {
                out += start..i
                start = i + 1
            }
        }
        return out
    }

    /**
     * Trim on word (maqaf-group) boundaries around the highlighted token. Never trims through
     * the token and never splits a maqaf group. Highlight is BY wordIndex, never by string match.
     */
    fun trim(words: List<Token>, highlight: Int, maxWords: Int = MAX_WORDS): TrimmedLine {
        val gs = groups(words)
        var lo = gs.indexOfFirst { highlight in it }.coerceAtLeast(0)
        var hi = lo
        var n = gs.getOrNull(lo)?.let { it.last - it.first + 1 } ?: 0
        var growLeft = true
        while (true) {
            val canL = lo > 0
            val canR = hi < gs.lastIndex
            if (!canL && !canR) break
            val pickLeft = if (canL && canR) growLeft else canL
            val g = if (pickLeft) gs[lo - 1] else gs[hi + 1]
            val size = g.last - g.first + 1
            if (n + size > maxWords) break
            if (pickLeft) lo-- else hi++
            n += size
            growLeft = !growLeft
        }
        val from = gs.getOrNull(lo)?.first ?: 0
        val to = gs.getOrNull(hi)?.last ?: words.lastIndex
        val shown = (from..to).map { i ->
            RowWord(i, displayHe(words[i]), i == highlight, joinsNext(words[i]) && i < to)
        }
        return TrimmedLine(shown, from > 0, to < words.lastIndex)
    }

    fun render(verse: Verse, wordIndex: Int, ketivQere: Boolean): RenderedRow {
        val token = verse.words[wordIndex]
        val aram = ConcordanceRules.lang(token) == 'A'
        val badges = buildList {
            if (aram) add("Aramaic")
            if (ketivQere) add("qere")
        }
        return RenderedRow(
            ref = BookTitles.verseLabel(verse.book, verse.chapter, verse.verse),
            hebrew = trim(verse.words, wordIndex),
            // JPS verbatim — identical to the reading line (Sofer MEDIUM-1, v0.7.1).
            english = verse.english.text,
            // Aramaic keeps the pack's [aramaic-pending]; never Hebrew SBL for A rows.
            phonetic = if (aram) "[aramaic-pending]" else token.phonetic,
            badges = badges,
            ketivOnTap = if (ketivQere) token.ketiv?.let { "Ketiv (written): $it" } else null
        )
    }

    /** Header strings: dictionary headword + id, scope sub-label, stored count, sanitized gloss. */
    fun header(entry: ConcordanceEntry, gloss: Gloss?): List<String> {
        val dn = entry.lemmaId.startsWith("H3068") || entry.lemmaId.startsWith("H3069")
        val probe = Token(
            he = entry.headword ?: "", lemmaId = entry.lemmaId, phonetic = "",
            glossId = entry.lemmaId, divineName = dn, morph = entry.lang.toString()
        )
        val hw = entry.headword?.let { displayHe(probe) } ?: "—"
        return listOf(
            hw,
            entry.lemmaId + if (entry.isAramaic) " · Aramaic" else "",
            GlossDisplay.forToken(probe, gloss).primary,
            ConcordanceRules.countLabel(entry.count),
            ConcordanceRules.SCOPE_LABEL
        )
    }
}
