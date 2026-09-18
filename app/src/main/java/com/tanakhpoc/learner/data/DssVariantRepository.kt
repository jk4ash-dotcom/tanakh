package com.tanakhpoc.learner.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * Offline DSS variant-note pack. Missing / empty / all-ship:false → no UI chrome.
 * Uses the same gz/plain resilience as [PackRepository].
 */
class DssVariantRepository private constructor(
    val pack: DssVariantPack,
    private val resolved: List<DssResolvedNote>
) {
    /** True when at least one note would show UI (ship && !advanced). */
    val hasVisibleNotes: Boolean
        get() = visibleNotes.isNotEmpty()

    val visibleNotes: List<DssResolvedNote>
        get() = resolved.filter { it.ship && !it.advanced }

    fun notesForVerse(osis: String): List<DssResolvedNote> =
        resolved.filter { it.osis == osis }.sortedBy { it.priority }

    fun visibleNotesForVerse(osis: String): List<DssResolvedNote> =
        notesForVerse(osis).filter { it.ship && !it.advanced }

    fun hasVisibleForVerse(osis: String): Boolean =
        visibleNotesForVerse(osis).isNotEmpty()

    companion object {
        const val ASSET_GZ = "data/dss_variants.json.gz"
        const val ASSET_PLAIN = "data/dss_variants.json"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        @Volatile
        private var instance: DssVariantRepository? = null

        fun empty(): DssVariantRepository =
            DssVariantRepository(DssVariantPack(), emptyList())

        /** Pure parse for unit tests. */
        fun parse(text: String): DssVariantRepository {
            val pack = json.decodeFromString(DssVariantPack.serializer(), text)
            val resolved = pack.notes.map { note ->
                DssResolvedNote(note, DssPlacementDeriver.derive(note))
            }
            return DssVariantRepository(pack, resolved)
        }

        /**
         * Hard-fail when a word-anchor snapshot drifts from [verse].words.
         * Returns null if OK, else an error message.
         */
        fun driftCheck(note: DssVariantNote, verse: Verse?): String? {
            if (note.anchor.type.lowercase() != "word") return null
            val idx = note.anchor.wordIndex
                ?: return "word anchor ${note.id}: missing wordIndex"
            if (verse == null) return "word anchor ${note.id}: verse ${note.osis} not loaded"
            if (idx !in verse.words.indices) {
                return "word anchor ${note.id}: wordIndex $idx out of range (size=${verse.words.size})"
            }
            val token = verse.words[idx]
            val he = note.anchor.he
            val lemma = note.anchor.lemmaId
            if (he != null && he != token.he) {
                return "word anchor ${note.id}: he drift at $idx expected=$he actual=${token.he}"
            }
            if (lemma != null && lemma != token.lemmaId) {
                return "word anchor ${note.id}: lemmaId drift at $idx expected=$lemma actual=${token.lemmaId}"
            }
            return null
        }

        suspend fun load(context: Context): DssVariantRepository {
            instance?.let { return it }
            return withContext(Dispatchers.IO) {
                instance ?: synchronized(this@Companion) {
                    instance ?: loadOnce(context.applicationContext).also { instance = it }
                }
            }
        }

        private fun loadOnce(context: Context): DssVariantRepository {
            val opener: (String) -> InputStream = { path -> context.assets.open(path) }
            val candidates = PackRepository.assetCandidates(ASSET_GZ, ASSET_PLAIN)
            return try {
                val text = PackRepository.readAssetText(opener, *candidates.toTypedArray())
                parse(text)
            } catch (_: FileNotFoundException) {
                empty()
            } catch (_: Exception) {
                // Corrupt / missing after aapt2 quirks → silent empty (no chrome).
                empty()
            }
        }

        fun clearInstanceForTests() {
            instance = null
        }
    }
}
