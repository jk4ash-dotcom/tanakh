package com.tanakhpoc.learner.navigation

object Routes {
    const val HOME = "home"
    const val ABOUT = "about"
    const val BOOK = "book/{book}"
    const val CHAPTER = "chapter/{book}/{chapter}"
    const val VERSE = "verse/{verseId}?w={w}"
    const val CONCORDANCE = "concordance/{key}"
    fun book(book: String) = "book/$book"
    fun chapter(book: String, chapter: Int) = "chapter/$book/$chapter"
    fun verse(verseId: String, wordIndex: Int = -1) = "verse/$verseId?w=$wordIndex"
    fun concordance(key: String) = "concordance/${android.net.Uri.encode(key)}"
}
