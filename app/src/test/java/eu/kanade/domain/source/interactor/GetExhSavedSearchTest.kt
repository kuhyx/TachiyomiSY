package eu.kanade.domain.source.interactor

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import exh.log.xLogE
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.interactor.GetSavedSearchById
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceId
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import xyz.nulldev.ts.api.http.serializer.FilterSerializer

internal class GetExhSavedSearchTest {

    private val getSavedSearchById = mockk<GetSavedSearchById>()
    private val getSavedSearchBySourceId = mockk<GetSavedSearchBySourceId>()
    private val serializer = FilterSerializer()
    private val interactor = GetExhSavedSearch(getSavedSearchById, getSavedSearchBySourceId, serializer)

    private class Title : Filter.Text("Title")

    private fun filters(): FilterList = FilterList(Title())

    private fun search(id: Long, query: String? = "q", filtersJson: String? = null): SavedSearch =
        SavedSearch(id = id, source = 7, name = "Search $id", query = query, filtersJson = filtersJson)

    @BeforeEach
    fun setUp() {
        mockkStatic("exh.log.LoggingThrowablesKt")
        every { interactor.xLogE(any<String>(), any<Throwable>()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun awaitOneIsNullForUnknownIds() = runTest {
        coEvery { getSavedSearchById.awaitOrNull(1) } returns null
        interactor.awaitOne(1) { filters() }.shouldBeNull()
    }

    @Test
    fun awaitOneAppliesTheFilters() = runTest {
        val serialized = serializer.serialize(FilterList(Title().apply { state = "saved" })).toString()
        coEvery { getSavedSearchById.awaitOrNull(1) } returns search(1, filtersJson = serialized)
        val result = interactor.awaitOne(1) { filters() }!!
        result.id shouldBe 1L
        result.name shouldBe "Search 1"
        result.query shouldBe "q"
        (result.filterList!!.single() as Filter.Text).state shouldBe "saved"
    }

    @Test
    fun blankQueryAndNoFiltersAreNull() = runTest {
        coEvery { getSavedSearchBySourceId.await(7) } returns listOf(search(2, query = " "), search(5, query = null))
        interactor.await(7) { filters() } shouldBe listOf(
            EXHSavedSearch(id = 2, name = "Search 2", query = null, filterList = null),
            EXHSavedSearch(id = 5, name = "Search 5", query = null, filterList = null),
        )
    }

    @Test
    fun unreadableFiltersAreLogged() = runTest {
        every { getSavedSearchBySourceId.subscribe(7) } returns flowOf(
            listOf(search(3, filtersJson = "not json"), search(4, filtersJson = """[{"type":"Unknown"}]""")),
        )
        val result = interactor.subscribe(7) { filters() }.first()
        result.map { it.filterList } shouldBe listOf(null, null)
        verify(exactly = 2) { interactor.xLogE("Failed to load saved search!", any<Throwable>()) }
    }
}
