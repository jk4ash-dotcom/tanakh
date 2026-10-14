package com.tanakhpoc.learner.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** One list slot. Pages always yield exactly their row count of slots (no mapNotNull drift). */
sealed interface RowSlot {
    data class Ready(val verseId: String, val wordIndex: Int, val rendered: RenderedRow) : RowSlot
    /** Row in the index whose verse/word did not resolve — shown as an unavailable row, never dropped. */
    data class Missing(val verseId: String, val wordIndex: Int) : RowSlot
}

sealed interface PageState {
    data object Loading : PageState
    data class Loaded(val slots: List<RowSlot>) : PageState
    data class Failed(val message: String) : PageState
}

/**
 * Critic M1–M3: per-page Deferred (no duplicate loads), whole loader on [dispatcher]
 * (Default), CancellationException rethrown, other failures → [PageState.Failed] + [retry].
 */
class ConcordancePager(
    val entry: ConcordanceEntry,
    private val scope: CoroutineScope,
    val pageSize: Int = 30,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val resolveVerse: suspend (verseId: String) -> Verse?
) {
    private val _pages = MutableStateFlow<Map<Int, PageState>>(emptyMap())
    val pages: StateFlow<Map<Int, PageState>> = _pages
    private val inFlight = HashMap<Int, Deferred<PageState>>()

    /** Number of loads actually started (test instrumentation for de-dup). */
    @Volatile var loadsStarted = 0
        private set

    val pageCount: Int get() = (entry.count + pageSize - 1) / pageSize

    fun pageOf(index: Int): Int = index / pageSize

    fun slot(index: Int): RowSlot? =
        (_pages.value[pageOf(index)] as? PageState.Loaded)?.slots?.getOrNull(index % pageSize)

    /** Request a page; returns the in-flight/completed Deferred. Loaded pages are not reloaded. */
    fun request(page: Int): Deferred<PageState>? {
        if (page !in 0 until pageCount) return null
        synchronized(inFlight) {
            inFlight[page]?.let { return it }
            val d = scope.async(dispatcher) { loadPage(page) }
            inFlight[page] = d
            loadsStarted++
            _pages.update { it + (page to PageState.Loading) }
            d.invokeOnCompletion { cause ->
                val state = when {
                    cause is CancellationException -> null
                    cause != null -> PageState.Failed(cause.message ?: cause.toString())
                    else -> d.getCompleted()
                }
                synchronized(inFlight) {
                    // Failed/cancelled pages can be requested again (retry row).
                    if (state !is PageState.Loaded) inFlight.remove(page)
                }
                _pages.update { if (state == null) it - page else it + (page to state) }
            }
            return d
        }
    }

    fun retry(page: Int): Deferred<PageState>? {
        synchronized(inFlight) {
            val cur = _pages.value[page]
            if (cur is PageState.Loaded || cur is PageState.Loading) return inFlight[page]
        }
        return request(page)
    }

    private suspend fun loadPage(page: Int): PageState = withContext(dispatcher) {
        try {
            val start = page * pageSize
            val expected = minOf(pageSize, entry.count - start)
            val rows = entry.page(start, pageSize)
            val slots = rows.map { r ->
                val v = resolveVerse(r.verseId)
                v?.let { ConcordanceDisplay.render(it, r.wordIndex, r.ketivQere) }
                    ?.let { RowSlot.Ready(r.verseId, r.wordIndex, it) }
                    ?: RowSlot.Missing(r.verseId, r.wordIndex)
            }.toMutableList()
            // Keep list alignment even if the index row string is short/malformed.
            while (slots.size < expected) slots += RowSlot.Missing("?", -1)
            PageState.Loaded(slots)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PageState.Failed(e.message ?: e.toString())
        }
    }
}

/** Concordance load state for the verse screen (Critic M3: explicit failure, no forever-spinner). */
sealed interface ConcordanceLoad {
    data object Loading : ConcordanceLoad
    data class Ready(val index: ConcordanceIndex) : ConcordanceLoad
    data class Failed(val message: String) : ConcordanceLoad

    /** Gloss-card link: null = still loading; failure → Unavailable (link hidden). */
    fun linkFor(token: Token): ConcordanceLink? = when (this) {
        Loading -> null
        is Ready -> index.linkFor(token)
        is Failed -> ConcordanceLink.Unavailable
    }
}

/** Verse-jump focus: only a wordIndex that exists in the verse may scroll/flash. */
object FocusPolicy {
    fun validFocus(verse: Verse, wordIndex: Int): Int? = wordIndex.takeIf { it in verse.words.indices }
}
