package com.tanakhpoc.learner.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import com.tanakhpoc.learner.data.ConcordanceLoad
import com.tanakhpoc.learner.data.ConcordancePager
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CancellationException
import com.tanakhpoc.learner.data.ConcordanceEntry
import com.tanakhpoc.learner.ui.screens.CONCORDANCE_PAGE
import com.tanakhpoc.learner.ui.screens.ConcordanceScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tanakhpoc.learner.TanakhApp
import com.tanakhpoc.learner.data.BookTitles
import com.tanakhpoc.learner.data.ChapterIndex
import com.tanakhpoc.learner.ui.screens.AboutScreen
import com.tanakhpoc.learner.ui.screens.BookScreen
import com.tanakhpoc.learner.ui.screens.ChapterScreen
import com.tanakhpoc.learner.ui.screens.HomeScreen
import com.tanakhpoc.learner.ui.screens.VerseScreen

@Composable
fun TanakhNavGraph() {
    val navController = rememberNavController()
    val app = TanakhApp.from(LocalContext.current)
    val repo by app.repository.collectAsStateWithLifecycle()
    val dssRepo by app.dssRepository.collectAsStateWithLifecycle()
    val loadError by app.loadError.collectAsStateWithLifecycle()

    val ready = repo
    if (ready == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                if (loadError != null) {
                    Text("Could not load catalog", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        loadError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading Tanakh catalog…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        return
    }

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                books = ready.bookSummaries,
                scope = ready.scope,
                onOpenBook = { b -> navController.navigate(Routes.book(b)) },
                onAbout = { navController.navigate(Routes.ABOUT) }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(
                version = ready.metaVersion,
                scope = ready.scope,
                totals = ready.catalog.totals,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.BOOK,
            arguments = listOf(navArgument("book") { type = NavType.StringType })
        ) { e ->
            val book = e.arguments?.getString("book") ?: return@composable
            var chapters by remember(book) { mutableStateOf<List<ChapterIndex>?>(null) }
            var err by remember(book) { mutableStateOf<String?>(null) }
            LaunchedEffect(book) {
                runCatching { ready.ensureBook(book) }
                    .onSuccess { chapters = it.chapters }
                    .onFailure { err = it.message }
            }
            when {
                err != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(err ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                chapters == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading ${BookTitles.title(book)}…")
                    }
                }
                else -> BookScreen(
                    book = book,
                    chapters = chapters!!,
                    onBack = { navController.popBackStack() },
                    onOpenChapter = { b, c -> navController.navigate(Routes.chapter(b, c)) }
                )
            }
        }
        composable(
            Routes.CHAPTER,
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.IntType }
            )
        ) { e ->
            val book = e.arguments?.getString("book") ?: "Gen"
            val chapter = e.arguments?.getInt("chapter") ?: 1
            var readyChapter by remember(book, chapter) { mutableStateOf(false) }
            var err by remember(book, chapter) { mutableStateOf<String?>(null) }
            LaunchedEffect(book, chapter) {
                runCatching { ready.ensureBook(book) }
                    .onSuccess { readyChapter = true }
                    .onFailure { err = it.message }
            }
            when {
                err != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(err ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                !readyChapter -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> ChapterScreen(
                    book = book,
                    chapter = chapter,
                    verses = ready.versesInChapter(book, chapter),
                    onBack = { navController.popBackStack() },
                    onOpenVerse = { id -> navController.navigate(Routes.verse(id)) }
                )
            }
        }
        composable(
            Routes.VERSE,
            arguments = listOf(
                navArgument("verseId") { type = NavType.StringType },
                navArgument("w") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { e ->
            val id = e.arguments?.getString("verseId") ?: return@composable
            val focusW = e.arguments?.getInt("w") ?: -1
            var conc by remember {
                mutableStateOf<ConcordanceLoad>(
                    ready.concordanceOrNull()?.let { ConcordanceLoad.Ready(it) } ?: ConcordanceLoad.Loading
                )
            }
            LaunchedEffect(Unit) {
                // Critic M3: explicit Failed state (load/hash error) → link Unavailable, no forever-spinner.
                conc = try {
                    ConcordanceLoad.Ready(ready.ensureConcordance())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ConcordanceLoad.Failed(e.message ?: e.toString())
                }
            }
            val book = id.substringBefore('.')
            var verseReady by remember(id) { mutableStateOf(ready.verse(id) != null && ready.glossesReady) }
            var err by remember(id) { mutableStateOf<String?>(null) }
            LaunchedEffect(id) {
                runCatching {
                    ready.ensureBook(book)
                    ready.ensureGlosses()
                }
                    .onSuccess { verseReady = true }
                    .onFailure { err = it.message }
            }
            when {
                err != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(err ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                !verseReady || ready.verse(id) == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> {
                    val verse = ready.verse(id)!!
                    val verseNotes = dssRepo?.visibleNotesForVerse(id, verse).orEmpty()
                    val bookNotes = dssRepo?.visibleBookNotes(book).orEmpty()
                    VerseScreen(
                        verse = verse,
                        resolveGloss = { ready.gloss(it) },
                        dssNotes = (bookNotes + verseNotes).distinctBy { it.id },
                        focusWordIndex = focusW,
                        concordanceLink = { t -> conc.linkFor(t) },
                        onSeeAll = { key -> navController.navigate(Routes.concordance(key)) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
        composable(
            Routes.CONCORDANCE,
            arguments = listOf(navArgument("key") { type = NavType.StringType })
        ) { e ->
            val key = e.arguments?.getString("key") ?: return@composable
            var entry by remember(key) { mutableStateOf<ConcordanceEntry?>(null) }
            var err by remember(key) { mutableStateOf<String?>(null) }
            val listState = rememberLazyListState()
            LaunchedEffect(key) {
                runCatching {
                    ready.ensureGlosses()
                    ready.ensureConcordance().entry(key) ?: error("No concordance entry $key")
                }.onSuccess { entry = it }.onFailure { err = it.message }
            }
            val loaded = entry
            when {
                err != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(err ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                loaded == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> {
                    val scope = rememberCoroutineScope()
                    val pager = remember(loaded) {
                        ConcordancePager(loaded, scope, CONCORDANCE_PAGE) { vid ->
                            ready.ensureBook(vid.substringBefore('.'))
                            ready.verse(vid)
                        }
                    }
                    ConcordanceScreen(
                    entry = loaded,
                    gloss = ready.gloss(loaded.lemmaId),
                    listState = listState,
                    pager = pager,
                    onOpenRow = { vid, wi -> navController.navigate(Routes.verse(vid, wi)) },
                    onBack = { navController.popBackStack() }
                )
                }
            }
        }
    }
}
