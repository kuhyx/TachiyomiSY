package eu.kanade.tachiyomi.ui.history

import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

internal class HistoryRemovalTest {
    private val harness = HistoryHarness()

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    @Test
    fun entriesAreRemoved() {
        val model = harness.model()
        val entry = history(1, null)
        model.removeFromHistory(entry)
        model.removeAllFromHistory(4L)
        coVerify(timeout = 5_000) { harness.removeHistory.await(entry) }
        coVerify(timeout = 5_000) { harness.removeHistory.await(4L) }
    }

    @Test
    fun clearingEmitsAnEvent() {
        coEvery { harness.removeHistory.awaitAll() } returns true
        val model = harness.model()
        model.removeAllHistory()
        model.events.await { true } shouldBe HistoryScreenModel.Event.HistoryCleared
    }

    @Test
    fun failedClearingIsSilent() {
        coEvery { harness.removeHistory.awaitAll() } returns false
        val model = harness.model()
        model.removeAllHistory()
        coVerify(timeout = 5_000) { harness.removeHistory.awaitAll() }
        runBlocking { withTimeoutOrNull(200) { model.events.first() } } shouldBe null
    }
}
