package exh.recs.sources

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.FakeDelegateSource
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.md.similar.MangaDexSimilarPagingSource
import exh.pref.DelegateSourcePreferences
import exh.source.mangaDexSourceIds
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.data.source.NoResultsException
import tachiyomi.i18n.sy.SYMR

/** A tracker-backed source whose remote calls are replaced by [byId] and [bySearch]. */
private class StubTrackerSource(
    private val byId: () -> List<SManga>,
    private val bySearch: () -> List<SManga>,
    override val associatedTrackerId: Long? = 1L,
) : TrackerRecommendationPagingSource("https://stub/", sourceManga(title = "Needle")) {
    override val name: String = "Stub"
    val searched: MutableList<String> = mutableListOf()
    val fetchedIds: MutableList<String> = mutableListOf()

    override suspend fun getRecsBySearch(search: String): List<SManga> {
        searched += search
        return bySearch()
    }

    override suspend fun getRecsById(id: String): List<SManga> {
        fetchedIds += id
        return byId()
    }
}

internal class RecommendationPagingSourceTest {
    private val server = CannedServer()
    private val stub = RecsStub(server.client)
    private val recs = listOf(SManga(url = "/r", title = "Rec"))

    @AfterEach
    fun tearDown() = stub.uninstall()

    @Test
    fun defaultsOfTheBaseClass() {
        val source = StubTrackerSource({ recs }, { recs })
        source.category shouldBe SYMR.strings.similar_titles
        source.associatedSourceId shouldBe null
    }

    @Test
    fun searchWhenUntracked() {
        val source = StubTrackerSource({ recs }, { recs })
        stub.tracks = listOf(track(trackerId = 2L))
        runBlocking { source.requestNextPage(1) } shouldBe MangasPage(recs, false)
        source.searched shouldContainExactly listOf("Needle")
        source.fetchedIds.isEmpty() shouldBe true
        // A source with no tracker of its own never matches, however the manga is tracked.
        val untracked = StubTrackerSource({ recs }, { recs }, associatedTrackerId = null)
        runBlocking { untracked.requestNextPage(1) }.mangas shouldBe recs
        untracked.searched shouldContainExactly listOf("Needle")
    }

    @Test
    fun idWhenTracked() {
        val source = StubTrackerSource({ recs }, { recs })
        stub.tracks = listOf(track(trackerId = 1L, remoteId = 8L))
        runBlocking { source.requestNextPage(1) }.mangas shouldBe recs
        source.fetchedIds shouldContainExactly listOf("8")
        source.searched.isEmpty() shouldBe true
    }

    @Test
    fun emptyAndFailingResults() {
        val empty = StubTrackerSource({ emptyList() }, { emptyList() })
        shouldThrow<NoResultsException> { runBlocking { empty.requestNextPage(1) } }
        val failing = StubTrackerSource({ error("boom") }, { error("boom") })
        shouldThrow<IllegalStateException> { runBlocking { failing.requestNextPage(1) } }.message shouldBe "boom"
    }

    @Test
    fun createSourcesWithoutDelegates() {
        stub.serve(DelegateSourcePreferences(eu.kanade.tachiyomi.source.online.MemoPreferenceStore()))
        val plain = mockk<Source> {
            every { id } returns 5L
            every { name } returns "Plain"
        }
        val sources = RecommendationPagingSource.createSources(sourceManga(), plain)
        sources.map { it.name } shouldContainExactly listOf("AniList", "MangaUpdates", "MangaUpdates", "MyAnimeList")
        sources.map { it.category.resourceId } shouldContainExactly listOf(
            SYMR.strings.community_recommendations.resourceId,
            SYMR.strings.community_recommendations.resourceId,
            SYMR.strings.similar_titles.resourceId,
            SYMR.strings.community_recommendations.resourceId,
        )
    }

    @Test
    fun createSourcesWithComick() {
        stub.serve(DelegateSourcePreferences(eu.kanade.tachiyomi.source.online.MemoPreferenceStore()))
        val comick = mockk<Source> {
            every { id } returns 6L
            every { name } returns "Comick"
        }
        RecommendationPagingSource.createSources(sourceManga(), comick).map { it.name } shouldContainExactly
            listOf("AniList", "Comick", "MangaUpdates", "MangaUpdates", "MyAnimeList")
    }

    @Test
    fun createSourcesWithMangaDex() {
        val preferenceStore = eu.kanade.tachiyomi.source.online.MemoPreferenceStore()
        val delegatePreferences = DelegateSourcePreferences(preferenceStore)
        stub.serve(delegatePreferences)
        val mangaDex = mockk<MangaDex>(relaxed = true)
        every { mangaDex.id } returns 8L
        every { mangaDex.name } returns "MangaDex"
        // `isMdBasedSource` reads the global list the source manager fills while loading extensions.
        val previousIds = mangaDexSourceIds
        mangaDexSourceIds = listOf(8L)
        val withDex = RecommendationPagingSource.createSources(sourceManga(), mangaDex)
        withDex.filterIsInstance<MangaDexSimilarPagingSource>().size shouldBe 1
        delegatePreferences.delegateSources.set(false)
        val withoutDex = RecommendationPagingSource.createSources(sourceManga(), mangaDex)
        withoutDex.filterIsInstance<MangaDexSimilarPagingSource>().isEmpty() shouldBe true
        mangaDexSourceIds = previousIds
    }

    @Test
    fun nonMdSourceIsNotDelegated() {
        stub.serve(DelegateSourcePreferences(eu.kanade.tachiyomi.source.online.MemoPreferenceStore()))
        val plain = FakeDelegateSource("http://plain.example", name = "Plain")
        val sources = RecommendationPagingSource.createSources(sourceManga(), plain)
        sources.filterIsInstance<MangaDexSimilarPagingSource>().isEmpty() shouldBe true
    }
}
