package com.tanakhpoc.learner.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * Offline catalog + per-book lazy packs (gzip assets) + shared gloss catalog.
 * Loads off the main thread.
 */
class PackRepository private constructor(
    val catalog: Catalog,
    private val glosses: Map<String, Gloss>,
    private val books: MutableMap<String, Pack>,
    private val assetOpener: (String) -> java.io.InputStream
) {
    val metaVersion: String get() = catalog.version
    val scope: String get() = catalog.scope
    val bookSummaries: List<CatalogBook> get() = catalog.books

    fun chaptersFor(book: String): List<ChapterIndex> =
        books[book]?.chapters ?: emptyList()

    fun verse(id: String): Verse? {
        val book = id.substringBefore('.')
        return books[book]?.verses?.firstOrNull { it.id == id }
    }

    fun gloss(id: String?): Gloss? = id?.let { glosses[it] }

    fun versesInChapter(book: String, chapter: Int): List<Verse> =
        books[book]?.verses?.filter { it.book == book && it.chapter == chapter } ?: emptyList()

    fun isBookLoaded(book: String): Boolean = books.containsKey(book)

    suspend fun ensureBook(book: String): Pack = withContext(Dispatchers.IO) {
        books[book]?.let { return@withContext it }
        bookMutex.withLock {
            books[book]?.let { return@withLock it }
            val summary = catalog.books.find { it.osis == book }
                ?: error("Unknown book $book")
            val asset = summary.assetGz?.removePrefix("data/") 
                ?: summary.asset.removePrefix("data/")
            val path = if (asset.startsWith("books/")) "data/$asset" else "data/$asset"
            val text = readAssetText(path)
            val pack = json.decodeFromString(Pack.serializer(), text)
            books[book] = pack
            pack
        }
    }

    private fun readAssetText(path: String): String {
        assetOpener(path).use { raw ->
            val stream = if (path.endsWith(".gz")) GZIPInputStream(raw) else raw
            return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        }
    }

    private val bookMutex = Mutex()

    companion object {
        private const val CATALOG = "data/catalog.json"
        private const val GLOSSES_GZ = "data/glosses.json.gz"

        @Volatile
        private var instance: PackRepository? = null

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /** Pure parse for unit tests (single combined pack fixture). */
        fun parseCombined(text: String): PackRepository {
            val pack = json.decodeFromString(Pack.serializer(), text)
            val catalog = Catalog(
                name = pack.meta.name,
                version = pack.meta.version,
                scope = pack.meta.scope,
                books = pack.chapters
                    .map { it.book }
                    .distinct()
                    .map { osis ->
                        val chs = pack.chapters.filter { it.book == osis }
                        CatalogBook(
                            osis = osis,
                            title = BookTitles.title(osis),
                            chapters = chs.size,
                            verses = pack.verses.count { it.book == osis }
                        )
                    },
                totals = CatalogTotals(
                    books = pack.chapters.map { it.book }.distinct().size,
                    verses = pack.verses.size,
                    glosses = pack.glosses.size
                )
            )
            val byBook = pack.verses.groupBy { it.book }.mapValues { (book, verses) ->
                Pack(
                    meta = pack.meta.copy(book = book, title = BookTitles.title(book)),
                    chapters = pack.chapters.filter { it.book == book },
                    verses = verses,
                    glosses = emptyMap()
                )
            }.toMutableMap()
            return PackRepository(catalog, pack.glosses, byBook) { error("no assets in parseCombined") }
        }

        /** @deprecated use parseCombined */
        fun parse(text: String): PackRepository = parseCombined(text)

        suspend fun load(context: Context): PackRepository {
            instance?.let { return it }
            return withContext(Dispatchers.IO) {
                instance ?: synchronized(this@Companion) {
                    instance ?: run {
                        val appCtx = context.applicationContext
                        val opener: (String) -> java.io.InputStream = { path ->
                            appCtx.assets.open(path)
                        }
                        val catalogText = opener(CATALOG).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val catalog = json.decodeFromString(Catalog.serializer(), catalogText)
                        val glossText = opener(GLOSSES_GZ).use { gz ->
                            GZIPInputStream(gz).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        }
                        val glosses: Map<String, Gloss> = json.decodeFromString(glossText)
                        PackRepository(catalog, glosses, mutableMapOf(), opener).also { instance = it }
                    }
                }
            }
        }

        fun clearInstanceForTests() {
            instance = null
        }
    }
}
