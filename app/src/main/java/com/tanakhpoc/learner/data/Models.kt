package com.tanakhpoc.learner.data

import kotlinx.serialization.Serializable

@Serializable
data class Catalog(
    val name: String,
    val version: String,
    val generatedAt: String = "",
    val scope: String = "",
    val navOrder: String = "Jewish Tanakh",
    val hebrewPin: String = "",
    val books: List<CatalogBook> = emptyList(),
    val glossesAsset: String = "data/glosses.json",
    val glossesAssetGz: String? = "data/glosses.json.gz",
    val totals: CatalogTotals = CatalogTotals()
)

@Serializable
data class CatalogBook(
    val osis: String,
    val title: String,
    val division: String = "",
    val jewishOrder: Int = 0,
    val chapters: Int = 0,
    val verses: Int = 0,
    val asset: String = "",
    val assetGz: String? = null,
    val aramaic: Boolean = false
)

@Serializable
data class CatalogTotals(
    val books: Int = 0,
    val verses: Int = 0,
    val glosses: Int = 0,
    val ketivQere: Int = 0,
    val gaps: Int = 0
)

@Serializable
data class Pack(
    val meta: PackMeta,
    val chapters: List<ChapterIndex>,
    val verses: List<Verse>,
    val glosses: Map<String, Gloss> = emptyMap()
)

@Serializable
data class PackMeta(
    val name: String,
    val version: String,
    val generatedAt: String = "",
    val scope: String = "",
    val book: String = "",
    val title: String = "",
    val division: String = "",
    val aramaic: Boolean = false,
    val hebrew: Map<String, String> = emptyMap(),
    val english: Map<String, String> = emptyMap(),
    val phonetics: Map<String, String> = emptyMap(),
    val glosses: Map<String, String> = emptyMap(),
    val display: Map<String, String> = emptyMap(),
    val gaps: List<String> = emptyList()
)

@Serializable
data class ChapterIndex(
    val book: String,
    val chapter: Int,
    val verseIds: List<String>
)

@Serializable
data class Verse(
    val id: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val english: EnglishLine,
    val words: List<Token>
)

@Serializable
data class EnglishLine(
    val text: String,
    val source: String = "JPS 1917",
    val license: String = "Public Domain",
    val kjvRef: String? = null,
    val versificationMapped: Boolean = false
)

@Serializable
data class Token(
    val he: String,
    val lemmaId: String? = null,
    val lemmaRaw: String? = null,
    val morph: String? = null,
    val phonetic: String,
    val glossId: String? = null,
    val divineName: Boolean = false,
    val procliticNote: String? = null,
    val ketiv: String? = null,
    val qereFlag: Boolean = false,
    val aramaic: Boolean = false
)

@Serializable
data class Gloss(
    val id: String,
    val primary: String,
    val senses: List<String> = emptyList(),
    val source: String = "",
    val definition: String? = null,
    val note: String? = null
)
