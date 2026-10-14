package com.tanakhpoc.learner

import com.tanakhpoc.learner.data.ConcordanceDisplay
import com.tanakhpoc.learner.data.ConcordanceHashMismatch
import com.tanakhpoc.learner.data.ConcordanceIndex
import com.tanakhpoc.learner.data.ConcordanceLink
import com.tanakhpoc.learner.data.ConcordanceLoad
import com.tanakhpoc.learner.data.ConcordancePager
import com.tanakhpoc.learner.data.FocusPolicy
import com.tanakhpoc.learner.data.PageState
import com.tanakhpoc.learner.data.RowSlot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Critic L7: paging (dedup, last page, failure/retry, missing verse, cancel), focus, failed-load link. */
class ConcordancePagerTest {
    companion object {
        @JvmStatic @BeforeClass fun load() = ConcordanceGateTest.load()
    }

    private val verses get() = ConcordanceGateTest.verses
    private fun fresh(key: String) = ConcordanceIndex(ConcordanceGateTest.index.file).entry(key)!!
    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Test
    fun concurrentRequests_singleLoad_allPagesNoDuplicates_lastPageSize() = runBlocking {
        val e = fresh("H:H0430")
        val p = ConcordancePager(e, scope(), 30) { verses[it] }
        val ds = (1..30).map { p.request(0)!! }
        ds.awaitAll()
        assertEquals("30 concurrent requests for page 0 → one load", 1, p.loadsStarted)
        (0 until p.pageCount).map { p.request(it)!! }.awaitAll()
        assertEquals(p.pageCount, p.loadsStarted)
        val slots = (0 until p.pageCount).flatMap { (p.pages.value[it] as PageState.Loaded).slots }
        assertEquals(e.count, slots.size)
        val ids = slots.map { (it as RowSlot.Ready).let { r -> "${r.verseId}:${r.wordIndex}" } }
        assertEquals(ids.size, ids.toSet().size)
        val last = (p.pages.value[p.pageCount - 1] as PageState.Loaded).slots
        assertEquals(e.count - (p.pageCount - 1) * 30, last.size)
        assertNull(p.request(p.pageCount)) // past the end
        assertEquals(e.count - 1, ids.indices.last)
        assertTrue(p.slot(e.count - 1) is RowSlot.Ready)
    }

    @Test
    fun failure_givesFailedState_thenRetryLoads() = runBlocking {
        val calls = AtomicInteger()
        val p = ConcordancePager(fresh("H:H3068"), scope(), 30) {
            if (calls.getAndIncrement() == 0) throw java.io.IOException("disk") else verses[it]
        }
        val st = p.request(0)!!.await()
        assertTrue(st is PageState.Failed)
        withTimeout(2000) { while (p.pages.value[0] !is PageState.Failed) Thread.sleep(5) }
        val again = p.retry(0)!!.await()
        assertTrue(again is PageState.Loaded)
        assertEquals(30, (again as PageState.Loaded).slots.size)
    }

    @Test
    fun missingVerse_keepsAlignmentAsMissingSlot() = runBlocking {
        val e = fresh("H:H3068")
        val firstId = e.page(0, 1).first().verseId
        val p = ConcordancePager(e, scope(), 30) { if (it == firstId) null else verses[it] }
        val st = p.request(0)!!.await() as PageState.Loaded
        assertEquals(30, st.slots.size)
        assertTrue(st.slots[0] is RowSlot.Missing)
        assertTrue(st.slots.drop(1).all { it is RowSlot.Ready })
    }

    @Test
    fun cancellation_isNotAFailure_andPageCanReload() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val s = scope()
        val p = ConcordancePager(fresh("H:H0430"), s, 30) { gate.await(); verses[it] }
        p.request(0)
        s.cancel()
        withTimeout(2000) { while (p.pages.value.containsKey(0)) Thread.sleep(5) }
        val p2 = ConcordancePager(fresh("H:H0430"), scope(), 30) { verses[it] }
        assertTrue(p2.request(0)!!.await() is PageState.Loaded)
    }

    @Test
    fun offsetCache_avoidsRescanFromZero() {
        val e = fresh("H:H3068")
        e.page(6000, 30)
        val before = e.scanSteps.get()
        e.page(6030, 30)
        assertEquals("next page starts from cached offset", before, e.scanSteps.get())
        e.page(3000, 30) // scans from cached 0 only up to 3000
        assertTrue(e.scanSteps.get() - before <= 3000)
        assertEquals(90, e.rowsDecoded)
    }

    @Test
    fun badWordIndex_renderNull_focusRejected() {
        val v = verses["Gen.1.1"]!!
        assertNull(ConcordanceDisplay.render(v, 99, false))
        assertNull(ConcordanceDisplay.render(v, -1, false))
        assertNull(FocusPolicy.validFocus(v, 7))
        assertNull(FocusPolicy.validFocus(v, -1))
        assertEquals(6, FocusPolicy.validFocus(v, 6))
    }

    @Test
    fun glossCardLink_whenHashCheckFails_isUnavailable() {
        val text = com.tanakhpoc.learner.data.PackRepository.readAssetText(
            { p -> java.io.File(ConcordanceGateTest.data.parentFile, p).inputStream() }, "data/concordance.json.gz"
        )
        val load: ConcordanceLoad = try {
            ConcordanceLoad.Ready(ConcordanceIndex.parse(text, "deadbeef"))
        } catch (e: ConcordanceHashMismatch) {
            ConcordanceLoad.Failed(e.message!!)
        }
        val t = verses["Gen.1.1"]!!.words[2]
        assertTrue(load is ConcordanceLoad.Failed)
        assertEquals(ConcordanceLink.Unavailable, load.linkFor(t))
        assertNull(ConcordanceLoad.Loading.linkFor(t))
        assertTrue(ConcordanceLoad.Ready(ConcordanceGateTest.index).linkFor(t) is ConcordanceLink.Lemma)
    }
}
