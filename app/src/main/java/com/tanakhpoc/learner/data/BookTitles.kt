package com.tanakhpoc.learner.data

/**
 * Jewish Tanakh navigation titles. Order for Torah checkpoint matches Jewish order.
 * Do not sort by Christian/OSHB filename order in the UI.
 */
object BookTitles {
    private val titles = mapOf(
        "Gen" to "Genesis",
        "Exod" to "Exodus",
        "Lev" to "Leviticus",
        "Num" to "Numbers",
        "Deut" to "Deuteronomy",
        "Josh" to "Joshua",
        "Judg" to "Judges",
        "1Sam" to "1 Samuel",
        "2Sam" to "2 Samuel",
        "1Kgs" to "1 Kings",
        "2Kgs" to "2 Kings",
        "Isa" to "Isaiah",
        "Jer" to "Jeremiah",
        "Ezek" to "Ezekiel",
        "Hos" to "Hosea",
        "Joel" to "Joel",
        "Amos" to "Amos",
        "Obad" to "Obadiah",
        "Jonah" to "Jonah",
        "Mic" to "Micah",
        "Nah" to "Nahum",
        "Hab" to "Habakkuk",
        "Zeph" to "Zephaniah",
        "Hag" to "Haggai",
        "Zech" to "Zechariah",
        "Mal" to "Malachi",
        "Ps" to "Psalms",
        "Prov" to "Proverbs",
        "Job" to "Job",
        "Song" to "Song of Songs",
        "Ruth" to "Ruth",
        "Lam" to "Lamentations",
        "Eccl" to "Ecclesiastes",
        "Esth" to "Esther",
        "Dan" to "Daniel",
        "Ezra" to "Ezra",
        "Neh" to "Nehemiah",
        "1Chr" to "1 Chronicles",
        "2Chr" to "2 Chronicles",
    )

    fun title(osis: String): String = titles[osis] ?: osis

    fun chapterLabel(book: String, chapter: Int): String = "${title(book)} $chapter"

    fun verseLabel(book: String, chapter: Int, verse: Int): String =
        "${title(book)} $chapter:$verse"
}
