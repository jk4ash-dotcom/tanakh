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
import androidx.compose.runtime.getValue
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
import com.tanakhpoc.learner.ui.screens.AboutScreen
import com.tanakhpoc.learner.ui.screens.ChapterScreen
import com.tanakhpoc.learner.ui.screens.HomeScreen
import com.tanakhpoc.learner.ui.screens.VerseScreen

@Composable
fun TanakhNavGraph() {
    val navController = rememberNavController()
    val app = TanakhApp.from(LocalContext.current)
    val repo by app.repository.collectAsStateWithLifecycle()
    val loadError by app.loadError.collectAsStateWithLifecycle()

    val ready = repo
    if (ready == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                if (loadError != null) {
                    Text("Could not load pack", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        loadError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading Genesis pack…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        return
    }

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                chapters = ready.chapters,
                onOpenChapter = { b, c -> navController.navigate(Routes.chapter(b, c)) },
                onAbout = { navController.navigate(Routes.ABOUT) }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(meta = ready.meta, onBack = { navController.popBackStack() })
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
            ChapterScreen(
                book = book,
                chapter = chapter,
                verses = ready.versesInChapter(book, chapter),
                onBack = { navController.popBackStack() },
                onOpenVerse = { id -> navController.navigate(Routes.verse(id)) }
            )
        }
        composable(
            Routes.VERSE,
            arguments = listOf(navArgument("verseId") { type = NavType.StringType })
        ) { e ->
            val id = e.arguments?.getString("verseId") ?: return@composable
            val verse = ready.verse(id) ?: return@composable
            VerseScreen(
                verse = verse,
                resolveGloss = { ready.gloss(it) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
