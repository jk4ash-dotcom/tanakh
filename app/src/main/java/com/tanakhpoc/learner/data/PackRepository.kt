package com.tanakhpoc.learner.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class PackRepository private constructor(private val pack: Pack) {
    val meta: PackMeta get() = pack.meta
    val chapters: List<ChapterIndex> get() = pack.chapters
    val verses: List<Verse> get() = pack.verses

    fun verse(id: String): Verse? = byId[id]
    fun gloss(id: String?): Gloss? = id?.let { pack.glosses[it] }
    fun versesInChapter(book: String, chapter: Int): List<Verse> =
        pack.verses.filter { it.book == book && it.chapter == chapter }

    private val byId = pack.verses.associateBy { it.id }

    companion object {
        private const val ASSET = "data/pack_gen_1_3.json"

        @Volatile
        private var instance: PackRepository? = null

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /** Pure parse for unit tests and IO loaders. */
        fun parse(text: String): PackRepository =
            PackRepository(json.decodeFromString(Pack.serializer(), text))

        /**
         * Loads pack off the main thread. Safe to call repeatedly; caches singleton.
         */
        suspend fun load(context: Context): PackRepository {
            instance?.let { return it }
            return withContext(Dispatchers.IO) {
                instance ?: synchronized(this) {
                    instance ?: run {
                        val text = context.applicationContext.assets
                            .open(ASSET)
                            .bufferedReader(Charsets.UTF_8)
                            .use { it.readText() }
                        parse(text).also { instance = it }
                    }
                }
            }
        }

        /** Test/reset only. */
        fun clearInstanceForTests() {
            instance = null
        }
    }
}
