package com.tanakhpoc.learner

import com.tanakhpoc.learner.data.DssPlacement
import com.tanakhpoc.learner.data.DssPlacementDeriver
import com.tanakhpoc.learner.data.DssVariantNote
import com.tanakhpoc.learner.data.DssVariantRepository
import com.tanakhpoc.learner.data.DssAnchor
import com.tanakhpoc.learner.data.EnglishLine
import com.tanakhpoc.learner.data.Token
import com.tanakhpoc.learner.data.Verse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DssVariantRepositoryTest {

    private val samplePackJson = """
    {
      "version": "0.1-stub",
      "status": "SAMPLE — not Sofer-signed",
      "notes": [
        {
          "id": "dss-isa-53-11-a",
          "osis": "Isa.53.11",
          "oshbBook": "Isa",
          "chapter": 53,
          "verse": 11,
          "anchor": { "type": "word", "wordIndex": 2, "he": "יִרְאֶה", "lemmaId": "H7200" },
          "priority": 1,
          "category": "plus",
          "mss": ["1QIsa_a"],
          "mtSummary": "SAMPLE",
          "dssHebrew": "",
          "dssSummary": "SAMPLE gloss",
          "uxLabel": "Qumran reading",
          "disclaimer": "Does not replace the Masoretic/OSHB text",
          "refs": [],
          "ship": false,
          "advanced": false
        },
        {
          "id": "dss-dan-seam-a",
          "osis": "Dan.2.4",
          "oshbBook": "Dan",
          "chapter": 2,
          "verse": 4,
          "anchor": { "type": "verse" },
          "priority": 5,
          "category": "language_seam",
          "mss": ["1QDan_a"],
          "dssHebrew": "",
          "dssSummary": "SAMPLE seam",
          "ship": false,
          "advanced": false
        },
        {
          "id": "dss-shipped-demo",
          "osis": "Gen.1.1",
          "oshbBook": "Gen",
          "chapter": 1,
          "verse": 1,
          "anchor": { "type": "verse" },
          "priority": 1,
          "category": "plus",
          "mss": ["1QGen"],
          "dssHebrew": "בראשית",
          "dssSummary": "Shipped demo note for tests only",
          "ship": true,
          "advanced": false
        }
      ]
    }
    """.trimIndent()

    @Test
    fun parse_sampleSeeds_andDerivePlacement() {
        val repo = DssVariantRepository.parse(samplePackJson)
        assertEquals(3, repo.pack.notes.size)

        val isa = repo.notesForVerse("Isa.53.11").single()
        assertEquals(DssPlacement.WORD_CHIP, isa.placement)
        assertEquals(2, isa.anchor.wordIndex)
        assertEquals("יִרְאֶה", isa.anchor.he)
        assertEquals("H7200", isa.anchor.lemmaId)
        assertFalse(isa.ship)

        val seam = repo.notesForVerse("Dan.2.4").single()
        assertEquals(DssPlacement.SEAM_MARKER, seam.placement)
    }

    @Test
    fun emptyPack_hidesUi() {
        val empty = DssVariantRepository.parse("""{"version":"x","notes":[]}""")
        assertFalse(empty.hasVisibleNotes)
        assertTrue(empty.visibleNotesForVerse("Gen.1.1").isEmpty())
    }

    @Test
    fun shipFalseOnly_hidesUiChrome() {
        val stubOnly = DssVariantRepository.parse(
            """
            {"version":"0.1-stub","status":"SAMPLE","notes":[{
              "id":"a","osis":"Isa.53.11","oshbBook":"Isa","chapter":53,"verse":11,
              "anchor":{"type":"word","wordIndex":2,"he":"יִרְאֶה","lemmaId":"H7200"},
              "category":"plus","mss":["1QIsa_a"],"dssHebrew":"","dssSummary":"S",
              "ship":false,"advanced":false
            }]}
            """.trimIndent()
        )
        assertFalse(stubOnly.hasVisibleNotes)
        assertTrue(stubOnly.visibleNotesForVerse("Isa.53.11").isEmpty())
        // Raw note still parseable for Sofer tooling
        assertEquals(1, stubOnly.notesForVerse("Isa.53.11").size)
    }

    @Test
    fun shipTrue_showsUi() {
        val repo = DssVariantRepository.parse(samplePackJson)
        assertTrue(repo.hasVisibleNotes)
        val vis = repo.visibleNotesForVerse("Gen.1.1")
        assertEquals(1, vis.size)
        assertEquals("dss-shipped-demo", vis[0].id)
        assertEquals("בראשית", vis[0].dssHebrew)
    }

    @Test
    fun advancedTrue_hiddenEvenIfShipped() {
        val repo = DssVariantRepository.parse(
            """
            {"notes":[{
              "id":"adv","osis":"Gen.1.1","oshbBook":"Gen","chapter":1,"verse":1,
              "anchor":{"type":"verse"},"category":"plus","mss":["x"],
              "dssHebrew":"","dssSummary":"orthography-ish","ship":true,"advanced":true
            }]}
            """.trimIndent()
        )
        assertFalse(repo.hasVisibleNotes)
    }

    @Test
    fun placement_literaryBookLevel_isBookBanner() {
        val note = DssVariantNote(
            id = "book",
            osis = "Jer.0.0",
            oshbBook = "Jer",
            chapter = 0,
            verse = 0,
            anchor = DssAnchor(type = "verse"),
            category = "literary",
            mss = listOf("4QJer_b"),
            dssSummary = "edition"
        )
        assertEquals(DssPlacement.BOOK_BANNER, DssPlacementDeriver.derive(note))
    }

    @Test
    fun placement_literaryVerse_isVerseIndicator() {
        val note = DssVariantNote(
            id = "jer",
            osis = "Jer.10.4",
            oshbBook = "Jer",
            chapter = 10,
            verse = 4,
            anchor = DssAnchor(type = "verse"),
            category = "literary",
            mss = listOf("4QJer_b"),
            dssSummary = "edition"
        )
        assertEquals(DssPlacement.VERSE_INDICATOR, DssPlacementDeriver.derive(note))
    }

    @Test
    fun driftCheck_hardFailOnHeMismatch() {
        val note = DssVariantNote(
            id = "dss-isa-53-11-a",
            osis = "Isa.53.11",
            oshbBook = "Isa",
            chapter = 53,
            verse = 11,
            anchor = DssAnchor(type = "word", wordIndex = 2, he = "יִרְאֶה", lemmaId = "H7200"),
            category = "plus",
            mss = listOf("1QIsa_a"),
            dssSummary = "x"
        )
        val verse = Verse(
            id = "Isa.53.11",
            book = "Isa",
            chapter = 53,
            verse = 11,
            english = EnglishLine(text = "x"),
            words = listOf(
                Token(he = "מֵעֲמַל", lemmaId = "H5999", phonetic = "a"),
                Token(he = "נַפְשׁוֹ", lemmaId = "H5315", phonetic = "b"),
                Token(he = "WRONG", lemmaId = "H7200", phonetic = "c")
            )
        )
        val err = DssVariantRepository.driftCheck(note, verse)
        assertTrue(err!!.contains("he drift"))
    }

    @Test
    fun visibleWordNotes_failClosedAgainstLiveVerse() {
        val repo = DssVariantRepository.parse(
            """
            {"notes":[{
              "id":"word","osis":"Isa.53.11","oshbBook":"Isa","chapter":53,"verse":11,
              "anchor":{"type":"word","wordIndex":0,"he":"יִרְאֶה","lemmaId":"H7200"},
              "category":"plus","mss":["1QIsa_a"],"dssHebrew":"אור","dssSummary":"s",
              "ship":true,"advanced":false
            }]}
            """.trimIndent()
        )
        val drifted = Verse(
            id = "Isa.53.11",
            book = "Isa",
            chapter = 53,
            verse = 11,
            english = EnglishLine(text = "x"),
            words = listOf(Token(he = "WRONG", lemmaId = "H7200", phonetic = "x"))
        )
        val matching = drifted.copy(words = listOf(Token(he = "יִרְאֶה", lemmaId = "H7200", phonetic = "x")))
        assertTrue(repo.visibleNotesForVerse("Isa.53.11", drifted).isEmpty())
        assertEquals(1, repo.visibleNotesForVerse("Isa.53.11", matching).size)
    }

    @Test
    fun driftCheck_okWhenSnapshotMatches() {
        val note = DssVariantNote(
            id = "dss-isa-53-11-a",
            osis = "Isa.53.11",
            oshbBook = "Isa",
            chapter = 53,
            verse = 11,
            anchor = DssAnchor(type = "word", wordIndex = 2, he = "יִרְאֶה", lemmaId = "H7200"),
            category = "plus",
            mss = listOf("1QIsa_a"),
            dssSummary = "x"
        )
        val verse = Verse(
            id = "Isa.53.11",
            book = "Isa",
            chapter = 53,
            verse = 11,
            english = EnglishLine(text = "x"),
            words = listOf(
                Token(he = "מֵעֲמַל", lemmaId = "H5999", phonetic = "a"),
                Token(he = "נַפְשׁוֹ", lemmaId = "H5315", phonetic = "b"),
                Token(he = "יִרְאֶה", lemmaId = "H7200", phonetic = "c")
            )
        )
        assertNull(DssVariantRepository.driftCheck(note, verse))
    }

    @Test
    fun sourceAsset_batch1HasExpectedShipCounts() {
        val roots = listOf(
            java.io.File("src/main/assets/data/dss_variants.json"),
            java.io.File("../app/src/main/assets/data/dss_variants.json"),
            java.io.File("app/src/main/assets/data/dss_variants.json")
        )
        val file = roots.firstOrNull { it.isFile }
            ?: error("dss_variants.json not found from cwd=${java.io.File(".").absolutePath}")
        val repo = DssVariantRepository.parse(file.readText(Charsets.UTF_8))
        assertTrue(repo.pack.notes.isNotEmpty())
        assertEquals("sofer-signed-batch1", repo.pack.status)
        assertEquals(16, repo.pack.notes.count { it.ship })
        assertEquals(4, repo.pack.notes.count { !it.ship })
        assertTrue("Batch 1 should expose visible UI chrome", repo.hasVisibleNotes)
        assertEquals(2, repo.notesForVerse("Isa.53.11").size)
        val isa = repo.notesForVerse("Isa.53.11").first { it.id == "dss-isa-53-11-a" }
        assertEquals(2, isa.anchor.wordIndex)
        assertEquals("יִרְאֶה", isa.anchor.he)
        assertEquals("H7200", isa.anchor.lemmaId)
        assertEquals(DssPlacement.WORD_CHIP, isa.placement)
    }

    @Test
    fun ignoreUnknownKeys_flexible() {
        val repo = DssVariantRepository.parse(
            """
            {"version":"1","extra":true,"notes":[{
              "id":"x","osis":"Gen.1.1","oshbBook":"Gen","chapter":1,"verse":1,
              "anchor":{"type":"verse","oshbWordIds":["ignored-until-pack-has-ids"]},
              "category":"plus","mss":[],"dssHebrew":"","dssSummary":"s",
              "ship":false,"futureField":99
            }]}
            """.trimIndent()
        )
        assertEquals(1, repo.pack.notes.size)
    }
}
