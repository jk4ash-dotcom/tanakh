package com.tanakhpoc.learner

import com.tanakhpoc.learner.data.Gloss
import com.tanakhpoc.learner.data.GlossDisplay
import com.tanakhpoc.learner.data.PackRepository
import com.tanakhpoc.learner.data.Token
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class PackSanityTest {
    companion object {
        private lateinit var repo: PackRepository

        @JvmStatic
        @BeforeClass
        fun loadPack() {
            val stream = PackSanityTest::class.java.classLoader!!
                .getResourceAsStream("data/pack_gen_1_3.json")
                ?: error("Missing test resource data/pack_gen_1_3.json")
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            repo = PackRepository.parse(text)
        }
    }

    @Test
    fun gen11_tokenOrderAndPhonetics() {
        val v = repo.verse("Gen.1.1")!!
        assertEquals(7, v.words.size)
        assertEquals("בְּרֵאשִׁית", v.words[0].he)
        assertEquals("bĕrēʾshîth", v.words[0].phonetic)
        assertEquals("בָּרָא", v.words[1].he)
        assertEquals("bārāʾ", v.words[1].phonetic)
        assertEquals("אֱלֹהִים", v.words[2].he)
        assertEquals("ʾĕlōhîm", v.words[2].phonetic)
        assertEquals("אֵת", v.words[3].he)
        assertEquals("הַשָּׁמַיִם", v.words[4].he)
        assertEquals("וְאֵת", v.words[5].he)
        assertEquals("הָאָרֶץ", v.words[6].he)
        assertEquals("hāʾārets", v.words[6].phonetic)
        v.words.forEach { t ->
            assertTrue(t.phonetic.isNotBlank())
            assertFalse(t.phonetic.contains(" "))
        }
    }

    @Test
    fun yhwh_hardRule_consonantsAndPhonetic() {
        val tokens = repo.verses.flatMap { it.words }.filter { it.divineName }
        assertTrue(tokens.isNotEmpty())
        tokens.forEach { t ->
            assertEquals("יהוה", t.he)
            assertEquals("YHWH", t.phonetic)
            assertEquals("H3068", t.lemmaId)
            assertFalse(Regex("[aeiouAEIOUĕâîōû]").containsMatchIn(t.phonetic))
        }
        val g24 = repo.verse("Gen.2.4")!!.words.first { it.divineName }
        assertEquals("יהוה", g24.he)
        assertEquals("YHWH", g24.phonetic)
    }

    @Test
    fun gloss_resolve_knownIds() {
        val g7225 = repo.gloss("H7225")
        assertNotNull(g7225)
        assertEquals("H7225", g7225!!.id)
        assertTrue(
            g7225.primary.contains("beginning", ignoreCase = true) ||
                g7225.primary.contains("first", ignoreCase = true)
        )

        val g3068 = repo.gloss("H3068")
        assertNotNull(g3068)
        assertEquals("LORD", g3068!!.primary)

        assertNull(repo.gloss(null))
        assertNull(repo.gloss("H_DOES_NOT_EXIST"))
    }

    @Test
    fun ltr_order_invariant_displayTokensNeverReverses() {
        val verses = listOf(
            repo.verse("Gen.1.1")!!,
            repo.verse("Gen.1.2")!!,
            repo.verse("Gen.2.4")!!,
            repo.verse("Gen.3.1")!!,
            repo.verse("Gen.3.15")!!
        )
        verses.forEach { v ->
            val shown = GlossDisplay.displayTokens(v)
            assertEquals(v.words.size, shown.size)
            assertEquals(v.words.map { it.he }, shown.map { it.he })
            assertEquals(v.words.map { it.phonetic }, shown.map { it.phonetic })
            assertEquals(v.words, shown)
            assertEquals(v.words.first().he, shown.first().he)
            assertEquals(v.words.last().he, shown.last().he)
        }
    }

    @Test
    fun glossDisplay_yhwh_hidesJehovahDump() {
        val raw = repo.gloss("H3068")!!
        assertTrue(raw.definition!!.contains("Jehovah", ignoreCase = true))
        val token = Token(
            he = "יהוה",
            lemmaId = "H3068",
            phonetic = "YHWH",
            glossId = "H3068",
            divineName = true
        )
        val shown = GlossDisplay.forToken(token, raw)
        assertEquals("LORD", shown.primary)
        assertEquals(listOf("divine name — see About"), shown.senses)
        assertNull(shown.definition)
        assertNotNull(shown.policyNote)
        val blob = (shown.primary + shown.senses.joinToString() + (shown.definition ?: "")).lowercase()
        assertFalse(blob.contains("jehovah"))
        assertFalse(blob.contains("ye.ho.vah") || blob.contains("yehovah"))
        assertFalse(blob.contains("vowel pointings"))
    }

    @Test
    fun glossDisplay_stripsYeHovahFromElohimDefinition() {
        val raw = repo.gloss("H0430")!!
        val token = Token(he = "אֱלֹהִים", lemmaId = "H0430", phonetic = "ʾĕlōhîm", glossId = "H0430")
        val shown = GlossDisplay.forToken(token, raw)
        assertEquals("God", shown.primary)
        val def = shown.definition ?: ""
        assertFalse(def.contains("ye.ho.vah", ignoreCase = true))
        assertFalse(def.contains("Jehovah", ignoreCase = true))
        assertFalse(def.contains("yehovah", ignoreCase = true))
    }

    @Test
    fun glossDisplay_sanitizeDefinition_globalForbidden() {
        val bad = """
            Jehovah = "the existing One"
            1) the proper name of the one true God
            Another name of ye.ho.vah (יהוה)
            unpronounced except with the vowel pointings of a.do.na
        """.trimIndent()
        val cleaned = GlossDisplay.sanitizeDefinition(bad)
        assertNotNull(cleaned)
        assertFalse(cleaned!!.contains("Jehovah", ignoreCase = true))
        assertFalse(cleaned.contains("ye.ho.vah", ignoreCase = true))
        assertFalse(cleaned.contains("vowel pointings", ignoreCase = true))
        assertTrue(cleaned.contains("proper name"))
        assertEquals(
            "light\n1) illumination",
            GlossDisplay.sanitizeDefinition("light\n1) illumination")
        )
        assertNull(GlossDisplay.sanitizeDefinition("Jehovah = existing"))
        assertNull(GlossDisplay.sanitizeDefinition(null))
    }

    @Test
    fun glossDisplay_stripsAdonaiVowelSenseOnH3068Raw() {
        val polluted = Gloss(
            id = "H3068",
            primary = "LORD",
            senses = listOf(
                "LORD",
                "unpronounced except with the vowel pointings of a.do.na (Adonai \"Lord\" H0136)"
            ),
            source = "TBESH",
            definition = "Jehovah = existing"
        )
        val token = Token(
            he = "יהוה",
            lemmaId = "H3068",
            phonetic = "YHWH",
            glossId = "H3068",
            divineName = true
        )
        val shown = GlossDisplay.forToken(token, polluted)
        shown.senses.forEach { s ->
            assertFalse(s.contains("vowel", ignoreCase = true))
            assertFalse(s.contains("a.do.na", ignoreCase = true))
            assertFalse(s.contains("Jehovah", ignoreCase = true))
        }
        assertNull(shown.definition)
    }
}
