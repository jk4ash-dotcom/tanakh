package com.tanakhpoc.learner.navigation

object Routes {
    const val HOME = "home"
    const val ABOUT = "about"
    const val CHAPTER = "chapter/{book}/{chapter}"
    const val VERSE = "verse/{verseId}"
    fun chapter(book: String, chapter: Int) = "chapter/$book/$chapter"
    fun verse(verseId: String) = "verse/$verseId"
}
