package eu.kanade.domain.manga.interactor

import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.async.coroutines.awaitAsList
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.Excluded_scanlatorsQueries
import tachiyomi.data.subscribeToList

/** Both scanlator interactors read the same query, so they share the stubbed database. */
internal class ExcludedScanlatorsTest {

    private val queries = mockk<Excluded_scanlatorsQueries>()
    private val database = mockk<Database> { every { excluded_scanlatorsQueries } returns queries }
    private val query = mockk<Query<String>>()

    @BeforeEach
    fun setUp() {
        mockkStatic("app.cash.sqldelight.async.coroutines.QueryExtensionsKt")
        mockkStatic("tachiyomi.data.QueryExtensionKt")
        every { queries.getExcludedScanlatorsByMangaId(4) } returns query
        coEvery { query.awaitAsList() } returns listOf("a", "b", "a")
        every { query.subscribeToList() } returns flowOf(listOf("a", "b", "a"))
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getReturnsDistinctScanlators() = runTest {
        val interactor = GetExcludedScanlators(database)
        interactor.await(4) shouldBe setOf("a", "b")
        interactor.subscribe(4).first() shouldBe setOf("a", "b")
    }

    @Test
    fun setAppliesTheDifference() = runTest {
        coEvery { database.transaction(any(), any()) } coAnswers {
            secondArg<suspend SuspendingTransactionWithoutReturn.() -> Unit>().invoke(mockk())
        }
        coEvery { queries.insert(4, "c") } returns 1L
        coEvery { queries.remove(4, setOf("b")) } returns 1L
        SetExcludedScanlators(database).await(4, setOf("a", "c"))
        coVerify(exactly = 1) { queries.insert(4, "c") }
        coVerify(exactly = 1) { queries.remove(4, setOf("b")) }
    }
}
