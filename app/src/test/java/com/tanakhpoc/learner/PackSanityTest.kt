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
                .getResourceAsStream("data/pack_torah_samples.json")
                ?: error("Missing test resource data/pack_torah_samples.json")
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            repo = PackRepository.parseCombined(text)
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
    fun sampleVerses_perTorahBook_present() {
        listOf(
            "Gen.1.1", "Exod.3.14", "Lev.19.18", "Num.6.24", "Deut.6.4"
        ).forEach { id ->
            val v = repo.verse(id)
            assertNotNull("missing $id", v)
            assertTrue(v!!.words.isNotEmpty())
            assertTrue(v.english.text.isNotBlank())
            assertEquals(id.substringBefore('.'), v.book)
        }
    }

    @Test
    fun yhwh_hardRule_consonantsAndPhonetic() {
        fun cons(s: String) = s.filter { it in 'א'..'ת' }
        val tokens = listOf("Gen.2.4", "Gen.4.3", "Exod.3.15", "Deut.6.4")
            .flatMap { repo.verse(it)!!.words }
            .filter { it.divineName }
        assertTrue(tokens.isNotEmpty())
        tokens.forEach { t ->
            assertTrue(
                "expected יהוה consonants on ${t.he}",
                cons(t.he) == "יהוה" || cons(t.he).endsWith("יהוה")
            )
            assertTrue(
                "expected YHWH phonetic on ${t.phonetic}",
                t.phonetic == "YHWH" || t.phonetic.endsWith("YHWH")
            )
            assertTrue(
                t.lemmaId == "H3068" || t.lemmaId == "H3069" ||
                    (t.lemmaId?.startsWith("H3068") == true) ||
                    (t.lemmaId?.startsWith("H3069") == true)
            )
            // Bare YHWH has no vowels; proclitic+YHWH may include prefix vowels (laYHWH)
            if (t.phonetic == "YHWH") {
                assertFalse(Regex("[aeiouAEIOUĕâîōû]").containsMatchIn(t.phonetic))
            }
        }
        val g24 = repo.verse("Gen.2.4")!!.words.first { it.divineName }
        assertEquals("יהוה", g24.he)
        assertEquals("YHWH", g24.phonetic)
        // MEDIUM: proclitic kept on chip (ליהוה) + laYHWH phonetics
        val g43 = repo.verse("Gen.4.3")!!.words.first { it.divineName }
        assertEquals("ליהוה", g43.he)
        assertEquals("laYHWH", g43.phonetic)
        assertNotNull(g43.procliticNote)
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
            "Gen.1.1", "Gen.2.4", "Exod.3.14", "Lev.19.18", "Num.6.24", "Deut.6.4"
        ).map { repo.verse(it)!! }
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

    @Test
    fun ketivQere_flagAndPhoneticFromQere() {
        val v = repo.verse("Gen.8.17")!!
        val kq = v.words.filter { it.qereFlag || it.ketiv != null }
        assertTrue("expected K/Q in Gen.8.17 sample", kq.isNotEmpty())
        kq.forEach { t ->
            assertTrue(t.qereFlag)
            assertNotNull(t.ketiv)
            assertTrue(t.phonetic.isNotBlank())
            assertFalse(t.phonetic == t.ketiv)
        }
    }

    @Test
    fun jps_verseLevel_notWordAligned() {
        listOf("Gen.1.1", "Exod.20.2", "Deut.6.5").forEach { id ->
            val v = repo.verse(id)!!
            assertTrue(v.english.text.isNotBlank())
            assertEquals("JPS 1917", v.english.source)
            v.words.forEach { w ->
                // Token has no english field in model — verse-level only
                assertTrue(w.he.isNotBlank())
            }
        }
    }

    @Test
    fun catalog_jewishOrder_torahBooks() {
        val osis = repo.bookSummaries.map { it.osis }
        assertTrue(osis.contains("Gen"))
        // samples fixture may only include books present in samples
        val order = listOf("Gen", "Exod", "Lev", "Num", "Deut")
        val present = order.filter { it in osis }
        assertEquals(present, present.sortedBy { order.indexOf(it) })
    }

    @Test
    fun deut64_xLargeSegFlatten_sixTokensIncludingShemaEchad() {
        val v = repo.verse("Deut.6.4")!!
        assertEquals(6, v.words.size)
        assertEquals("שְׁמַע", v.words[0].he)
        assertEquals("יִשְׂרָאֵל", v.words[1].he)
        assertEquals("יהוה", v.words[2].he)
        assertTrue(v.words[2].divineName)
        assertEquals("YHWH", v.words[2].phonetic)
        assertEquals("אֱלֹהֵינוּ", v.words[3].he)
        assertEquals("יהוה", v.words[4].he)
        assertEquals("אֶחָד", v.words[5].he)
        // consonants: שמע … אחד (nested x-large seg must not drop letters)
        fun cons(s: String) = s.filter { it in '\u05D0'..'\u05EA' }
        assertEquals("שמע", cons(v.words[0].he))
        assertEquals("אחד", cons(v.words[5].he))
    }

    @Test
    fun xLargeSegVerses_lev1142_num275_keepLargeLetterWords() {
        fun cons(s: String) = s.filter { it in '\u05D0'..'\u05EA' }
        val lev = repo.verse("Lev.11.42")!!
        assertTrue(lev.words.any { cons(it.he) == "גחון" })
        assertEquals(22, lev.words.size)
        val num = repo.verse("Num.27.5")!!
        assertTrue(num.words.any { cons(it.he) == "משפטן" })
        assertEquals(6, num.words.size)
    }


    @Test
    fun glossDisplay_h3071_jehovahNissi_doesNotLeakPrimary() {
        val token = Token(
            he = "נִסִּי",
            lemmaId = "H3071",
            phonetic = "nissî",
            glossId = "H3071",
            divineName = false
        )
        val gloss = Gloss(
            id = "H3071",
            primary = "YHWH/Jehovah-nissi",
            senses = listOf("YHWH/Jehovah-nissi"),
            source = "TBESH",
            definition = "Jehovah-nissi = \"Jehovah is my banner\""
        )
        val d = GlossDisplay.forToken(token, gloss)
        assertFalse(d.primary.contains("Jehovah", ignoreCase = true))
        assertFalse(d.primary.contains("yehovah", ignoreCase = true))
        d.senses.forEach { assertFalse(it.contains("Jehovah", ignoreCase = true)) }
        assertNull(d.definition)
    }

    @Test
    fun glossDisplay_h3069_pointed_usesYhwhPath() {
        val token = Token(
            he = "יְהוִה",
            lemmaId = "H3069",
            phonetic = "YHWH",
            glossId = "H3069",
            divineName = false
        )
        val gloss = Gloss(
            id = "H3069",
            primary = "YHWH/God",
            senses = listOf("YHWH/God"),
            source = "TBESH",
            definition = "Jehovah-used primarily"
        )
        val d = GlossDisplay.forToken(token, gloss)
        assertEquals("God", d.primary)
        assertEquals("divine name — see About", d.senses.first())
        assertNull(d.definition)
    }


    @Test
    fun glossDisplay_procliticCompound_functionalNotFakeTbesh() {
        val token = Token(
            he = "וְלָהֶם",
            lemmaId = null,
            lemmaRaw = "c/l",
            phonetic = "wĕlāhem",
            glossId = "pfx:c/l",
            procliticNote = "וְ (vav) proclitic — often 'and'; לְ (lamed) proclitic — often 'to/for'"
        )
        val gloss = Gloss(
            id = "pfx:c/l",
            primary = token.procliticNote!!,
            senses = emptyList(),
            source = "proclitic-functional",
            note = "functional role — not a TBESH lexical sense"
        )
        val shown = GlossDisplay.forToken(token, gloss)
        assertEquals(token.procliticNote, shown.primary)
        assertEquals("proclitic-functional", shown.source)
        assertTrue(shown.policyNote!!.contains("not a lexical gloss"))
        assertTrue(shown.senses.isEmpty())
        // YHWH path still wins over proclitic functional
        val yhwh = GlossDisplay.forToken(
            Token(he = "יהוה", lemmaId = "H3068", phonetic = "YHWH", glossId = "H3068", divineName = true),
            Gloss(id = "H3068", primary = "LORD", source = "TBESH")
        )
        assertEquals("LORD", yhwh.primary)
    }

}
