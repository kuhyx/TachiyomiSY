package tachiyomi.data.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.model.Source as DomainSource

internal class SourceRepositoryImplTest {
    private val env = InjektEnv()
    private val plain = mockSource(sourceId = 1L, latest = true)
    private val eh = mockSource(sourceId = EH_SOURCE_ID)
    private val exh = mockSource(sourceId = EXH_SOURCE_ID)
    private val http = mockk<HttpSource> {
        every { id } returns 2L
        every { name } returns "Http"
        every { lang } returns "ja"
        every { supportsLatest } returns false
    }
    private val sourceManager = mockk<SourceManager> {
        every { sources } returns flowOf(listOf(plain, http))
        every { getOrStub(1L) } returns plain
        every { getOrStub(EH_SOURCE_ID) } returns eh
        every { getOrStub(EXH_SOURCE_ID) } returns exh
    }
    private val repository = SourceRepositoryImpl(sourceManager, inMemoryDatabase())

    @BeforeEach
    fun setUp() {
        env.install()
    }

    @AfterEach
    fun tearDown() {
        env.restore()
    }

    @Test
    fun getSourcesMapsEveryInstalled() = runTest {
        repository.getSources().first() shouldBe listOf(
            DomainSource(id = 1L, lang = "en", name = "Source 1", supportsLatest = true, isStub = false),
            DomainSource(id = 2L, lang = "ja", name = "Http", supportsLatest = false, isStub = false),
        )
    }

    @Test
    fun onlineSourcesAreHttpOnly() = runTest {
        repository.getOnlineSources().first() shouldBe listOf(
            DomainSource(id = 2L, lang = "ja", name = "Http", supportsLatest = false, isStub = false),
        )
    }

    @Test
    fun searchEhSourceIsEhSpecific() {
        val filters = FilterList()

        val pagingSource = repository.search(EH_SOURCE_ID, "q", filters).shouldBeInstanceOf<EHentaiSearchPagingSource>()

        pagingSource.query shouldBe "q"
        pagingSource.filters shouldBe filters
    }

    @Test
    fun searchPlainSourceIsGeneric() {
        repository.search(1L, "q", FilterList()).shouldBeInstanceOf<SourceSearchPagingSource>()
    }

    @Test
    fun popularEhSourceIsEhSpecific() {
        repository.getPopular(EXH_SOURCE_ID).shouldBeInstanceOf<EHentaiPopularPagingSource>()
    }

    @Test
    fun popularPlainSourceIsGeneric() {
        repository.getPopular(1L).shouldBeInstanceOf<SourcePopularPagingSource>()
    }

    @Test
    fun latestEhSourceIsEhSpecific() {
        repository.getLatest(EH_SOURCE_ID).shouldBeInstanceOf<EHentaiLatestPagingSource>()
    }

    @Test
    fun latestPlainSourceIsGeneric() {
        repository.getLatest(1L).shouldBeInstanceOf<SourceLatestPagingSource>()
    }
}
