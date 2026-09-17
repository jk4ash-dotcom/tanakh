package com.tanakhpoc.learner.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tanakhpoc.learner.TanakhApp
import com.tanakhpoc.learner.data.PackRepository
import com.tanakhpoc.learner.ui.screens.AboutScreen
import com.tanakhpoc.learner.ui.screens.ChapterScreen
import com.tanakhpoc.learner.ui.screens.HomeScreen
import com.tanakhpoc.learner.ui.screens.VerseScreen

@Composable
fun TanakhNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repo = remember {
        runCatching { TanakhApp.from(context).repository }
            .getOrElse { PackRepository.getInstance(context) }
    }

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                chapters = repo.chapters,
                onOpenChapter = { b, c -> navController.navigate(Routes.chapter(b, c)) },
                onAbout = { navController.navigate(Routes.ABOUT) }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(meta = repo.meta, onBack = { navController.popBackStack() })
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
                verses = repo.versesInChapter(book, chapter),
                onBack = { navController.popBackStack() },
                onOpenVerse = { id -> navController.navigate(Routes.verse(id)) }
            )
        }
        composable(
            Routes.VERSE,
            arguments = listOf(navArgument("verseId") { type = NavType.StringType })
        ) { e ->
            val id = e.arguments?.getString("verseId") ?: return@composable
            val verse = repo.verse(id) ?: return@composable
            VerseScreen(
                verse = verse,
                resolveGloss = { repo.gloss(it) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
