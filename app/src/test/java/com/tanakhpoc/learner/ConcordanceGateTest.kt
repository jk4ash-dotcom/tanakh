package com.tanakhpoc.learner

import com.tanakhpoc.learner.data.Catalog
import com.tanakhpoc.learner.data.ConcordanceDisplay
import com.tanakhpoc.learner.data.ConcordanceHashMismatch
import com.tanakhpoc.learner.data.ConcordanceIndex
import com.tanakhpoc.learner.data.ConcordanceLink
import com.tanakhpoc.learner.data.ConcordanceRules
import com.tanakhpoc.learner.data.Gloss
import com.tanakhpoc.learner.data.GlossDisplay
import com.tanakhpoc.learner.data.Pack
import com.tanakhpoc.learner.data.PackRepository
import com.tanakhpoc.learner.data.Token
import com.tanakhpoc.learner.data.Verse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * Sofer concordance gate G1–G6 (HARD-FAIL) re-verified on the JVM against the
 * shipped assets, independent of the Node build. G4 writes reports/concordance_gate_G4.json.
 */
class ConcordanceGateTest {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        lateinit var root: File
        lateinit var data: File
        lateinit var catalog: Catalog
        lateinit var index: ConcordanceIndex
        lateinit var glosses: Map<String, Gloss>
        val verses = LinkedHashMap<String, Verse>()
        lateinit var packHash: String

        private fun readMaybeGz(base: File): ByteArray {
            val gz = File(base.path + ".gz")
            val f = if (gz.isFile) gz else base
            val raw = f.readBytes()
            return if (raw.size > 1 && raw[0] == 0x1f.toByte() && raw[1] == 0x8b.toByte())
                GZIPInputStream(ByteArrayInputStream(raw)).readBytes() else raw
        }

        @JvmStatic
        @BeforeClass
        fun load() {
            root = File(System.getProperty("tanakh.root") ?: "..").canonicalFile
            data = File(root, "app/src/main/assets/data")
            catalog = json.decodeFromString(Catalog.serializer(), File(data, "catalog.json").readText())
            val md = MessageDigest.getInstance("SHA-256")
            for (b in catalog.books) {
                val bytes = readMaybeGz(File(data, "books/${b.osis}.json"))
                md.update((b.osis + "\n").toByteArray()); md.update(bytes); md.update("\n".toByteArray())
                val pack = json.decodeFromString(Pack.serializer(), bytes.toString(Charsets.UTF_8))
                pack.verses.forEach { verses[it.id] = it }
            }
            packHash = md.digest().joinToString("") { "%02x".format(it) }
            val stamp = catalog.concordance ?: error("catalog missing concordance stamp")
            // Loader path: gz → plain fallback with magic sniff (aapt2 quirk).
            val text = PackRepository.readAssetText(
                { p -> File(data.parentFile, p).takeIf { it.isFile }?.inputStream() ?: throw FileNotFoundException(p) },
                *PackRepository.assetCandidates(stamp.assetGz, stamp.asset).toTypedArray()
            )
            index = ConcordanceIndex.parse(text, stamp.packHash)
            glosses = json.decodeFromString(readMaybeGz(File(data, "glosses.json")).toString(Charsets.UTF_8))
        }

        fun word(v: String, i: Int): Token? = verses[v]?.words?.getOrNull(i)
    }

    // ---------- G1 ----------
    @Test
    fun G1_countsSelfConsistent() {
        val total = verses.values.sumOf { it.words.size }
        var indexed = 0
        val seen = HashSet<String>()
        for (k in index.keys) {
            val e = index.entry(k)!!
            val rows = e.allRows()
            assertEquals("G1b $k", e.count, rows.size)
            indexed += rows.size
            rows.forEach { assertTrue("G1c dup ${it.verseId}:${it.wordIndex}", seen.add("${it.verseId}:${it.wordIndex}")) }
        }
        var particles = 0; var noLemma = 0
        verses.values.forEach { v -> v.words.forEach { w ->
            if (ConcordanceRules.isParticle(w)) particles++ else if (w.lemmaId == null) noLemma++
        } }
        assertEquals("G1a", total, indexed + particles + noLemma)
        assertEquals(index.file.totals.indexed, indexed)
        assertEquals(index.file.totals.excludedParticle, particles)
        assertEquals(particles, index.particles.values.sum())
    }

    @Test
    fun G1d_labelUsesStoredCount() {
        val t = verses["Gen.1.1"]!!.words[2] // אֱלֹהִים
        val link = index.linkFor(t) as ConcordanceLink.Lemma
        assertEquals(index.entry("H:H0430")!!.count, link.count)
        assertEquals("Appears ${link.count} times", ConcordanceRules.countLabel(link.count))
        val h = ConcordanceDisplay.header(index.entry("H:H0430")!!, glosses["H0430"])
        assertTrue(h.contains("Appears ${link.count} times"))
        assertTrue(h.contains("in this text (OSHB)"))
    }

    // ---------- G2 ----------
    @Test
    fun G2_roundTripAndHash() {
        assertEquals("G2 pack hash stamp (catalog)", packHash, catalog.concordance!!.packHash)
        assertEquals("G2 pack hash stamp (index)", packHash, index.packHash)
        for (k in index.keys) {
            val e = index.entry(k)!!
            for (r in e.allRows()) {
                val w = word(r.verseId, r.wordIndex) ?: run { fail("G2 unresolved $k ${r.verseId}:${r.wordIndex}"); return }
                assertEquals("G2 lemma $k ${r.verseId}", e.lemmaId, w.lemmaId)
                assertEquals("G2/G3 lang $k ${r.verseId}", e.lang, ConcordanceRules.lang(w))
            }
        }
    }

    @Test
    fun G2_loaderRefusesMismatchedHash() {
        val stamp = catalog.concordance!!
        val text = PackRepository.readAssetText(
            { p -> File(data.parentFile, p).takeIf { it.isFile }?.inputStream() ?: throw FileNotFoundException(p) },
            "data/concordance.json.gz"
        )
        try {
            ConcordanceIndex.parse(text, stamp.packHash.reversed()); fail("accepted stale index")
        } catch (_: ConcordanceHashMismatch) { }
        try {
            ConcordanceIndex.parse(text, null); fail("accepted unstamped catalog")
        } catch (_: ConcordanceHashMismatch) { }
    }

    @Test
    fun loader_plainFallbackWhenAapt2Gunzips() {
        val plain = GZIPInputStream(File(data, "concordance.json.gz").inputStream()).readBytes()
        val text = PackRepository.readAssetText(
            { p -> if (p.endsWith(".gz")) throw FileNotFoundException(p) else ByteArrayInputStream(plain) },
            *PackRepository.assetCandidates("data/concordance.json.gz", "data/concordance.json").toTypedArray()
        )
        assertEquals(index.packHash, ConcordanceIndex.parse(text, catalog.concordance!!.packHash).packHash)
    }

    // ---------- G3 ----------
    @Test
    fun G3_aramaicSplit() {
        for (vid in listOf("Gen.31.47", "Dan.2.4", "Dan.7.28", "Ezra.4.8", "Ezra.6.18", "Ezra.7.12")) {
            val v = verses[vid]!!
            val aram = v.words.withIndex().filter { it.value.morph?.startsWith("A") == true }
            assertTrue("G3 expected Aramaic tokens in $vid", aram.isNotEmpty())
            aram.filter { !ConcordanceRules.isParticle(it.value) && it.value.lemmaId != null }.forEach { (i, w) ->
                val a = index.entry("A:${w.lemmaId}")!!.allRows().any { it.verseId == vid && it.wordIndex == i }
                val h = index.entry("H:${w.lemmaId}")?.allRows()?.any { it.verseId == vid && it.wordIndex == i } ?: false
                assertTrue("G3 $vid:$i in A:", a); assertFalse("G3 $vid:$i in H:", h)
            }
        }
        // Gen.31.47 Aramaic rows render with badge + aramaic-pending phonetics
        val r = ConcordanceDisplay.render(verses["Gen.31.47"]!!, 3, false)
        assertTrue(r.badges.contains("Aramaic")); assertEquals("[aramaic-pending]", r.phonetic)
    }

    // ---------- G4 ----------
    private val forbidden = Regex("""(?i)jehovah|yahweh|yehovah|ye\.\s*ho\.\s*vah|a\.\s*do\.\s*na[iy]""")
    private val vocalizedYhwh = Regex("י[\u0591-\u05C7]+ה[\u0591-\u05C7]*ו[\u0591-\u05C7]*ה")

    @Test
    fun G4_divineNameRowsSanitized() {
        val dnKeys = index.keys.filter { it.substring(2).startsWith("H3068") || it.substring(2).startsWith("H3069") }
        val dnRowsByFlag = verses.values.flatMap { v -> v.words.withIndex().filter { it.value.divineName }.map { v.id to it.index } }
        val targets = LinkedHashSet<Pair<String, Int>>()
        dnKeys.forEach { k -> index.entry(k)!!.allRows().forEach { targets += it.verseId to it.wordIndex } }
        targets += dnRowsByFlag
        // Also every row whose JPS line carries a DN substitution, across all keys.
        verses.values.filter { forbidden.containsMatchIn(it.english.text) }.forEach { v ->
            v.words.indices.forEach { targets += v.id to it }
        }
        val hits = mutableListOf<String>()
        var rendered = 0
        for ((vid, wi) in targets) {
            val row = ConcordanceDisplay.render(verses[vid]!!, wi, verses[vid]!!.words[wi].ketiv != null)
            rendered++
            row.allStrings().forEach { s ->
                if (forbidden.containsMatchIn(s) || vocalizedYhwh.containsMatchIn(s)) hits += "$vid:$wi «$s»"
            }
        }
        val headers = dnKeys.flatMap { ConcordanceDisplay.header(index.entry(it)!!, glosses[it.substring(2)]) }
        headers.filter { forbidden.containsMatchIn(it) || vocalizedYhwh.containsMatchIn(it) }.forEach { hits += "header «$it»" }

        // Perf: H3068 first page decodes only that page.
        val fresh = ConcordanceIndex(index.file).entry("H:H3068")!!
        val t0 = System.nanoTime()
        val p0 = fresh.page(0, 30)
        val firstPageMs = (System.nanoTime() - t0) / 1e6
        val deep = fresh.page(6000, 30)
        val decodedAfter = fresh.rowsDecoded
        val report = """{
  "gate": "G4", "ok": ${hits.isEmpty() && decodedAfter == 60 && p0.size == 30},
  "packHash": "${index.packHash}",
  "dnKeys": [${dnKeys.joinToString { "\"$it\"" }}],
  "rowsRendered": $rendered,
  "forbiddenHits": [${hits.take(50).joinToString { "\"" + it.replace("\"", "'") + "\"" }}],
  "headerStrings": [${headers.joinToString { "\"$it\"" }}],
  "englishPolicy": "JPS verbatim in rows (== reading line). Adonai-x titles allowed (Gen.22.14, Exod.17.15, Judg.6.24); Exod.6.3 fixed to YHWH in the pack; no JPS line contains jehovah/yehovah/yahweh",
  "perf": {"H3068count": ${fresh.count}, "page0Rows": ${p0.size}, "page200Rows": ${deep.size}, "rowsDecodedTotal": $decodedAfter, "firstPageMs": ${"%.2f".format(firstPageMs)}}
}"""
        File(root, "reports/concordance_gate_G4.json").writeText(report)
        assertTrue("G4 forbidden DN text: ${hits.take(10)}", hits.isEmpty())
        assertEquals("G4 DN pagination must not decode all rows", 60, decodedAfter)
        assertTrue(fresh.count > 6000)
        assertTrue("G4 first page < 200ms", firstPageMs < 200)
    }

    @Test
    fun G4_jpsHasNoYhwhVocalization() {
        val bad = Regex("(?i)jehovah|yehovah|yahweh")
        val hits = verses.values.filter { bad.containsMatchIn(it.english.text) }.map { it.id }
        assertTrue("JPS vocalization in $hits", hits.isEmpty())
        assertTrue(verses["Exod.6.3"]!!.english.text.contains("by My name YHWH"))
        mapOf("Gen.22.14" to "Adonai-jireh", "Exod.17.15" to "Adonai-nissi", "Judg.6.24" to "'Adonai-shalom'").forEach { (id, s) ->
            assertTrue("$id keeps $s", verses[id]!!.english.text.contains(s))
            assertEquals(verses[id]!!.english.text, ConcordanceDisplay.render(verses[id]!!, 0, false).english)
        }
    }

    @Test
    fun glossCard_noYhwhVocalizationInAnyDisplayedGloss() {
        val bad = Regex("(?i)jehovah|yehovah|yahweh")
        val rawHits = glosses.values.count { g -> (listOf(g.primary, g.definition.orEmpty()) + g.senses).any { bad.containsMatchIn(it) } }
        assertTrue("expected raw Yahweh/Jehovah definitions to exercise sanitizer", rawHits > 0)
        val hits = mutableListOf<String>()
        for ((id, g) in glosses) {
            for (dn in listOf(false, true)) {
                val t = Token(he = "", lemmaId = id, glossId = id, phonetic = "", divineName = dn && id.startsWith("H3068"))
                val d = GlossDisplay.forToken(t, g)
                (listOf(d.primary, d.definition.orEmpty(), d.source, d.policyNote.orEmpty()) + d.senses)
                    .filter { bad.containsMatchIn(it) }.forEach { hits += "$id «$it»" }
            }
        }
        assertTrue("gloss card leaks: ${hits.take(10)}", hits.isEmpty())
    }

    @Test
    fun G2_verseIdFormat() {
        val re = Regex("^[1-3]?[A-Za-z]+\\.\\d+\\.\\d+$")
        for (k in index.keys) for (r in index.entry(k)!!.allRows()) assertTrue("bad verseId ${r.verseId}", re.matches(r.verseId))
    }

    @Test
    fun catalogVersionMatchesApp() {
        assertEquals("0.7.2-concordance-poc", catalog.version)
    }

    // ---------- G5 ----------
    @Test
    fun G5_ketivQere() {
        var ketivWords = 0; var ketivExcluded = 0
        verses.values.forEach { v -> v.words.forEach { w ->
            if (w.ketiv != null) { ketivWords++; if (ConcordanceRules.isParticle(w) || w.lemmaId == null) ketivExcluded++ }
        } }
        var kq = 0
        for (k in index.keys) for (r in index.entry(k)!!.allRows()) {
            val w = word(r.verseId, r.wordIndex)!!
            assertEquals("G5 flag ${r.verseId}:${r.wordIndex}", w.ketiv != null, r.ketivQere)
            if (r.ketivQere) kq++
        }
        assertEquals("G5 K/Q rows", ketivWords - ketivExcluded, kq)
        val jer = verses["Jer.48.44"]!!.words[0]
        assertEquals("הניס", jer.ketiv)
        val row = index.entry("H:${jer.lemmaId}")!!.allRows().first { it.verseId == "Jer.48.44" && it.wordIndex == 0 }
        assertTrue(row.ketivQere)
        val rendered = ConcordanceDisplay.render(verses["Jer.48.44"]!!, 0, true)
        assertTrue(rendered.badges.contains("qere"))
        assertEquals(jer.he, rendered.hebrew.words.first { it.highlighted }.text) // qere surface, by index
        assertEquals("Ketiv (written): הניס", rendered.ketivOnTap)
    }

    // ---------- G6 ----------
    @Test
    fun G6_particleRule() {
        for (k in index.keys) {
            assertFalse("G6 particle leaked $k", Regex("^[HA]:H9\\d{3}").containsMatchIn(k) || k.contains("pfx:"))
        }
        assertNotNull(index.entry("H:H0853")); assertNotNull(index.entry("H:H3605"))
        assertFalse(index.particles.containsKey("H:H0853")); assertFalse(index.particles.containsKey("H:H3605"))
        val lo = verses["Gen.31.47"]!!.words[1] // לוֹ H9005
        assertEquals(ConcordanceLink.FunctionWord, index.linkFor(lo))
        val et = verses["Gen.1.1"]!!.words[3] // אֵת H0853
        assertTrue(index.linkFor(et) is ConcordanceLink.Lemma)
    }

    // ---------- display rules ----------
    @Test
    fun display_highlightByIndexTrimAndMaqaf() {
        // Same form twice in one verse: Jer.48.44 has הַפַּחַת at 5 and 8 — highlight only the indexed one.
        val v = verses["Jer.48.44"]!!
        val l = ConcordanceDisplay.trim(v.words, 8)
        assertEquals(listOf(8), l.words.filter { it.highlighted }.map { it.wordIndex })
        // Long verses: trim on group boundaries, never through token or a maqaf group.
        val long = verses.values.filter { it.words.size > 30 }
        assertTrue(long.isNotEmpty())
        long.take(400).forEach { lv ->
            val groups = ConcordanceDisplay.groups(lv.words)
            listOf(0, lv.words.size / 2, lv.words.lastIndex).forEach { hi ->
                val t = ConcordanceDisplay.trim(lv.words, hi)
                val shown = t.words.map { it.wordIndex }.toSet()
                assertTrue(hi in shown)
                groups.forEach { g -> val n = g.count { it in shown }; assertTrue("split maqaf ${lv.id} $g", n == 0 || n == g.count()) }
                assertTrue(t.trimmedStart || t.trimmedEnd)
            }
        }
    }
}
