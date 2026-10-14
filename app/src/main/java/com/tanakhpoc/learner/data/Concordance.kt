package com.tanakhpoc.learner.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Hebrew/Aramaic concordance (Sofer gate spec 2026-10-13).
 * Index is built at pack time (tools/pipeline/build-concordance.mjs) keyed `lang:lemmaId`.
 * Rows are kept as the compact build string and decoded per page so DN (H3068,
 * 6.5k rows) never materialises every row up front.
 */
@Serializable
data class ConcordanceFile(
    val schema: Int = 1,
    val packVersion: String = "",
    val packHash: String,
    val totals: ConcordanceTotals = ConcordanceTotals(),
    val keys: Map<String, ConcordanceEntryRaw> = emptyMap(),
    val particles: Map<String, Int> = emptyMap()
)

@Serializable
data class ConcordanceTotals(
    val words: Int = 0,
    val indexed: Int = 0,
    val excludedParticle: Int = 0,
    val excludedNoLemma: Int = 0,
    val keys: Int = 0,
    val particleKeys: Int = 0
)

@Serializable
data class ConcordanceEntryRaw(val c: Int, val p: Boolean = false, val hw: String? = null, val r: String = "")

data class ConcordanceRow(val verseId: String, val wordIndex: Int, val ketivQere: Boolean)

class ConcordanceHashMismatch(expected: String, actual: String) :
    IllegalStateException("Concordance index pack hash $actual does not match catalog $expected — refusing stale index")

/** One lemma's concordance. [count] is the stored build count — the ONLY source of "Appears N times". */
class ConcordanceEntry(val key: String, private val raw: ConcordanceEntryRaw) {
    val count: Int get() = raw.c
    val headword: String? get() = raw.hw
    val lang: Char get() = key[0]
    val lemmaId: String get() = key.substring(2)
    val isAramaic: Boolean get() = lang == 'A'

    private val decoded = java.util.concurrent.atomic.AtomicInteger(0)

    /** Rows decoded so far (perf instrumentation for the DN pagination assert). Critic L1: atomic. */
    val rowsDecoded: Int get() = decoded.get()

    /** Critic L3: row index → char offset of that row's cell, filled as pages are decoded. */
    private val offsets = java.util.TreeMap<Int, Int>().apply { put(0, 0) }

    /** Number of comma-scan steps taken (test instrumentation for the offset cache). */
    val scanSteps = java.util.concurrent.atomic.AtomicInteger(0)

    /** Decode rows [start, start+size) by scanning the build string; never decodes the rest. */
    fun page(start: Int, size: Int): List<ConcordanceRow> {
        if (start < 0 || start >= count || size <= 0) return emptyList()
        val s = raw.r
        val (cachedRow, cachedPos) = synchronized(offsets) { offsets.floorEntry(start).let { it.key to it.value } }
        var pos = cachedPos
        var i = cachedRow
        while (i < start) {
            scanSteps.incrementAndGet()
            pos = s.indexOf(',', pos) + 1
            if (pos == 0) return emptyList()
            i++
        }
        val startPos = pos
        val out = ArrayList<ConcordanceRow>(minOf(size, count - start))
        while (out.size < size && pos < s.length) {
            val end = s.indexOf(',', pos).let { if (it < 0) s.length else it }
            out += decode(s.substring(pos, end))
            pos = end + 1
        }
        synchronized(offsets) {
            offsets[start] = startPos
            if (pos < s.length) offsets[start + out.size] = pos
        }
        decoded.addAndGet(out.size)
        return out
    }

    fun allRows(): List<ConcordanceRow> = page(0, count)

    private fun decode(cell: String): ConcordanceRow {
        val kq = cell.endsWith('k')
        val body = if (kq) cell.dropLast(1) else cell
        val colon = body.lastIndexOf(':')
        return ConcordanceRow(body.substring(0, colon), body.substring(colon + 1).toInt(), kq)
    }
}

/** Result for the gloss card link. */
sealed interface ConcordanceLink {
    data object Unavailable : ConcordanceLink
    data object FunctionWord : ConcordanceLink
    data class Lemma(val key: String, val count: Int) : ConcordanceLink
}

class ConcordanceIndex(val file: ConcordanceFile) {
    private val entries = HashMap<String, ConcordanceEntry>()
    val packHash: String get() = file.packHash
    val keys: Set<String> get() = file.keys.keys
    val particles: Map<String, Int> get() = file.particles

    fun entry(key: String): ConcordanceEntry? = synchronized(entries) {
        entries[key] ?: file.keys[key]?.let { ConcordanceEntry(key, it).also { e -> entries[key] = e } }
    }

    fun linkFor(token: Token): ConcordanceLink {
        if (ConcordanceRules.isParticle(token)) return ConcordanceLink.FunctionWord
        val key = ConcordanceRules.keyFor(token) ?: return ConcordanceLink.Unavailable
        val e = file.keys[key] ?: return ConcordanceLink.Unavailable
        return ConcordanceLink.Lemma(key, e.c)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /**
         * Parse + refuse a pack-hash mismatch (Sofer G2).
         * Critic L4 — what the runtime check does and does not prove: it compares the index's
         * stamped packHash with the hash the same build stamped into catalog.json. It catches a
         * stale/mismatched index asset shipped against a newer catalog. It does NOT re-hash the
         * book packs on device (too costly); true drift is caught at build time by
         * build-concordance.mjs (exit 1) and ConcordanceGateTest.G2 (recomputes from packs).
         */
        fun parse(text: String, expectedPackHash: String?): ConcordanceIndex {
            val f = json.decodeFromString(ConcordanceFile.serializer(), text)
            if (expectedPackHash.isNullOrBlank() || f.packHash != expectedPackHash) {
                throw ConcordanceHashMismatch(expectedPackHash ?: "(none)", f.packHash)
            }
            return ConcordanceIndex(f)
        }
    }
}

object ConcordanceRules {
    private val PARTICLE_LEMMA = Regex("^H9\\d{3}")

    fun lang(token: Token): Char = if (token.morph?.firstOrNull() == 'A') 'A' else 'H'

    /** Sofer G6: lemmaId in H9xxx OR the chip's gloss is a `pfx:` functional gloss. */
    fun isParticle(token: Token): Boolean =
        PARTICLE_LEMMA.containsMatchIn(token.lemmaId.orEmpty()) || token.glossId.orEmpty().startsWith("pfx:")

    fun keyFor(token: Token): String? = token.lemmaId?.let { "${lang(token)}:$it" }

    /** Count label — includes the current occurrence; always fed the stored count. */
    fun countLabel(count: Int): String = if (count == 1) "Appears 1 time" else "Appears $count times"

    const val SCOPE_LABEL = "in this text (OSHB)"
    const val FUNCTION_WORD = "Common function word — not listed in the concordance"
}
