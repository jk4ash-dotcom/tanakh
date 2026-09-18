package com.tanakhpoc.learner.data

import kotlinx.serialization.Serializable

/**
 * Dead Sea Scrolls variant-note pack (Phase-1 stub).
 * Schema locked: Sleuth content fields + Sofer attach keys.
 * See docs/DSS_VARIANT_NOTES.md.
 */

@Serializable
data class DssVariantPack(
    val version: String = "0.1-stub",
    val status: String = "",
    val notes: List<DssVariantNote> = emptyList()
)

@Serializable
data class DssVariantNote(
    val id: String,
    val osis: String,
    val oshbBook: String,
    val chapter: Int,
    val verse: Int,
    val anchor: DssAnchor = DssAnchor(type = "verse"),
    val priority: Int = 100,
    val category: String,
    val mss: List<String> = emptyList(),
    val mtSummary: String = "",
    val dssHebrew: String = "",
    val dssSummary: String = "",
    val uxLabel: String = DEFAULT_UX_LABEL,
    val disclaimer: String = DEFAULT_DISCLAIMER,
    val refs: List<String> = emptyList(),
    val ship: Boolean = false,
    val advanced: Boolean = false
) {
    companion object {
        const val DEFAULT_UX_LABEL = "Qumran reading"
        const val DEFAULT_DISCLAIMER = "Does not replace the Masoretic/OSHB text"
    }
}

@Serializable
data class DssAnchor(
    /** `word` | `verse` */
    val type: String,
    val wordIndex: Int? = null,
    val wordIndexEnd: Int? = null,
    /** Snapshot for hard-fail drift check when type=word. */
    val he: String? = null,
    val lemmaId: String? = null,
    /** Optional; ignored until pack exposes OSHB word ids. */
    val oshbWordIds: List<String> = emptyList()
)

/** Derived at load — not a JSON field. */
enum class DssPlacement {
    WORD_CHIP,
    SEAM_MARKER,
    BOOK_BANNER,
    VERSE_INDICATOR
}

/**
 * Note with loader-derived placement. [raw] is the serialized note.
 */
data class DssResolvedNote(
    val raw: DssVariantNote,
    val placement: DssPlacement
) {
    val id: String get() = raw.id
    val osis: String get() = raw.osis
    val ship: Boolean get() = raw.ship
    val advanced: Boolean get() = raw.advanced
    val category: String get() = raw.category
    val mss: List<String> get() = raw.mss
    val dssHebrew: String get() = raw.dssHebrew
    val dssSummary: String get() = raw.dssSummary
    val uxLabel: String get() = raw.uxLabel.ifBlank { DssVariantNote.DEFAULT_UX_LABEL }
    val disclaimer: String get() = raw.disclaimer.ifBlank { DssVariantNote.DEFAULT_DISCLAIMER }
    val mtSummary: String get() = raw.mtSummary
    val refs: List<String> get() = raw.refs
    val priority: Int get() = raw.priority
    val anchor: DssAnchor get() = raw.anchor
}

object DssPlacementDeriver {
    /**
     * word → word_chip;
     * verse + language_seam → seam_marker;
     * verse + literary + book-level → book_banner;
     * else → verse_indicator.
     */
    fun derive(note: DssVariantNote): DssPlacement {
        val type = note.anchor.type.lowercase()
        if (type == "word") return DssPlacement.WORD_CHIP
        if (type == "verse" && note.category.equals("language_seam", ignoreCase = true)) {
            return DssPlacement.SEAM_MARKER
        }
        if (type == "verse" &&
            note.category.equals("literary", ignoreCase = true) &&
            isBookLevel(note)
        ) {
            return DssPlacement.BOOK_BANNER
        }
        return DssPlacement.VERSE_INDICATOR
    }

    /** Book-level literary: verse == 0 or osis chapter/verse zeroed. */
    fun isBookLevel(note: DssVariantNote): Boolean {
        if (note.verse == 0) return true
        val parts = note.osis.split('.')
        if (parts.size >= 3 && parts[1] == "0" && parts[2] == "0") return true
        if (parts.size == 2 && parts[1] == "0") return true
        return false
    }
}
