package com.tanakhpoc.learner.data

import kotlinx.serialization.Serializable

@Serializable
data class Pack(
    val meta: PackMeta,
    val chapters: List<ChapterIndex>,
    val verses: List<Verse>,
    val glosses: Map<String, Gloss>
)

@Serializable
data class PackMeta(
    val name: String,
    val version: String,
    val generatedAt: String = "",
    val scope: String = "",
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
    val license: String = "Public Domain"
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
    val qereFlag: Boolean = false
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
