package exh.recs

import cafe.adriel.voyager.core.model.ScreenModelStore
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.readMember
import exh.pref.DelegateSourcePreferences
import exh.recs.sources.rankedResults
import exh.recs.sources.sourceManga
import exh.recs.sources.track
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.model.Track

private const val WAIT_MS = 45_000L
private const val MAL_RECS = """{"data":[{"entry":{"title":"Rec One","url":"https://mal/1"}}]}"""

@RunWith(RobolectricTestRunner::class)
internal class RecommendsScreenModelTest {
    private val harness = SourceTestHarness()
    private val server = CannedServer()
    private val manga = sourceManga(id = 4L, title = "Needle")
    private val getManga = mockk<GetManga>()
    private var tracks = emptyList<Track>()

    @Before
    fun setUp() {
        harness.install()
        harness.serve(DelegateSourcePreferences(harness.store))
        harness.serve(eu.kanade.domain.track.service.TrackPreferences(harness.store))
        harness.serve(eu.kanade.tachiyomi.data.track.TrackerManager())
        harness.serve<eu.kanade.tachiyomi.network.NetworkHelper>(
            eu.kanade.tachiyomi.source.online.networkHelperOf(server.client),
        )
        val getTracks = mockk<tachiyomi.domain.track.interactor.GetTracks>()
        coEvery { getTracks.await(any<Long>()) } answers { tracks }
        harness.serve(getTracks)
        coEvery { getManga.await(4L) } returns manga
        every { getManga.subscribe(any(), any()) } returns kotlinx.coroutines.flow.flowOf(manga)
        harness.serve(getManga)
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<Manga>()) } answers { firstArg() }
        harness.serve(networkToLocal)
        val source = mockk<Source> {
            every { id } returns 7L
            every { name } returns "Plain"
        }
        harness.serve<SourceManager>(mockk { every { getOrStub(any()) } returns source })
    }

    @After
    fun tearDown() {
        // Voyager caches a screen model's IO scope globally, keyed by the last screen key; a model
        // built outside a Navigator reuses that one entry, so it is dropped between tests.
        val dependencies = checkNotNull(ScreenModelStore.readMember(ScreenModelStore::class, "dependencies"))
        val remove = dependencies::class.java.methods.first { it.name == "remove" && it.parameterCount == 1 }
        (dependencies as Map<*, *>).keys.filter { "IoCoroutineScope" in it.toString() }.forEach { key ->
            remove.invoke(dependencies, key)
        }
        harness.uninstall()
    }

    private fun awaitSettled(model: RecommendsScreenModel): RecommendsScreenModel.State = runBlocking {
        withTimeout(WAIT_MS) { model.state.first { it.total > 0 && it.progress == it.total } }
    }

    @Test
    fun singleSourceLoadsEverySource() {
        // MyAnimeList answers, the other three fail to parse: one model covers both item states.
        server.answers = { request -> if (request.url.host == "api.jikan.moe") MAL_RECS else "not json" }
        tracks = listOf(track(trackerId = eu.kanade.tachiyomi.data.track.TrackerManager.MYANIMELIST))
        val model = RecommendsScreenModel(RecommendsScreen.Args.SingleSourceManga(mangaId = 4L, sourceId = 7L))
        val state = awaitSettled(model)
        state.title shouldBe "Needle"
        state.total shouldBe 4
        // Sources with results sort first, then by name and category.
        state.items.keys.map { it.name } shouldContainExactly
            listOf("MyAnimeList", "AniList", "MangaUpdates", "MangaUpdates")
        val success = state.items.values.filterIsInstance<RecommendationItemResult.Success>().single()
        success.result.map { it.title } shouldContainExactly listOf("Rec One")
        state.items.values.filterIsInstance<RecommendationItemResult.Error>().size shouldBe 3
        state.filteredItems.size shouldBe 4
    }

    @Test
    fun mergedResultsUseStaticSources() {
        val model = RecommendsScreenModel(RecommendsScreen.Args.MergedSourceMangas(listOf(rankedResults(2))))
        val state = awaitSettled(model)
        state.title.shouldBeNull()
        state.total shouldBe 1
        val success = state.items.values.single() as RecommendationItemResult.Success
        success.result.map { it.title } shouldContainExactly listOf("Manga 1", "Manga 2")
        success.isEmpty shouldBe false
    }

    @Test
    fun unresolvedSourcesPlaceholder() {
        val model = RecommendsScreenModel(
            RecommendsScreen.Args.MergedSourceMangas(listOf(rankedResults(1, associatedSourceId = null))),
        )
        val success = awaitSettled(model).items.values.single() as RecommendationItemResult.Success
        success.result.single().source shouldBe -1L
    }

    @Test
    fun resultVisibility() {
        RecommendationItemResult.Loading.isVisible(onlyShowHasResults = false) shouldBe true
        RecommendationItemResult.Loading.isVisible(onlyShowHasResults = true) shouldBe false
        val empty = RecommendationItemResult.Success(emptyList())
        empty.isEmpty shouldBe true
        empty.isVisible(onlyShowHasResults = true) shouldBe false
        val filled = RecommendationItemResult.Success(listOf(manga))
        filled.isVisible(onlyShowHasResults = true) shouldBe true
        filled.copy(result = emptyList()).isEmpty shouldBe true
        val error = RecommendationItemResult.Error(IllegalStateException("x"))
        error.isVisible(onlyShowHasResults = true) shouldBe false
        error.copy(throwable = IllegalStateException("y")).throwable.message shouldBe "y"
    }

    @Test
    fun stateDefaults() {
        val state = RecommendsScreenModel.State()
        state.title.shouldBeNull()
        state.items.isEmpty() shouldBe true
        state.progress shouldBe 0
        state.total shouldBe 0
        state.copy(title = "t").title shouldBe "t"
    }
}
