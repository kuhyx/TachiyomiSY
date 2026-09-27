package exh.recs.batch

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import exh.recs.sources.RecommendationPagingSource
import exh.recs.sources.TrackerRecommendationPagingSource
import exh.recs.sources.sourceManga
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track

internal const val FAKE_WAIT_MS = 30_000L

/** A recommendation source of this test's own, optionally tied to source [linkedTo]. */
internal class FakeRecsSource(
    override val name: String,
    private val linkedTo: Long? = null,
    private val answer: suspend () -> MangasPage,
) : RecommendationPagingSource(Manga.create()) {
    override val associatedSourceId: Long? get() = linkedTo

    override suspend fun requestNextPage(currentPage: Int): MangasPage = answer()
}

/** A tracker-backed recommendation source for tracker 5 that always recommends [urls]. */
internal class FakeTrackerSource(private val urls: List<String>) :
    TrackerRecommendationPagingSource("", sourceManga()) {
    override val name: String = "Tracked"
    override val associatedTrackerId: Long = 5L

    override suspend fun getRecsBySearch(search: String): List<SManga> = urls.map { SManga(url = it, title = it) }

    override suspend fun getRecsById(id: String): List<SManga> = getRecsBySearch(id)
}

/** A page of recommendations titled [titles], each with url "/<title>". */
internal fun pageOf(vararg titles: String): MangasPage =
    MangasPage(titles.map { SManga(url = "/$it", title = it) }, false)

/** A library row for [title] with id [id]. */
internal fun libraryRow(title: String, id: Long = 5L): LibraryManga = LibraryManga(
    manga = sourceManga(id = id, title = title),
    categories = listOf(0L),
    totalChapters = 0,
    readCount = 0,
    bookmarkCount = 0,
    latestUpload = 0,
    chapterFetchedAt = 0,
    lastRead = 0,
)

/** Injekt for [RecommendationSearchHelper] with [sources] as every manga's recommendation sources. */
internal class RecsSearchRig(private val harness: SourceTestHarness = SourceTestHarness()) {
    val preferences = SourcePreferences(harness.store)
    val sourceManager = mockk<SourceManager>()
    var library: List<LibraryManga> = listOf(libraryRow("Owned title"))
    var tracks: List<Track> = emptyList()
    var sources: () -> List<RecommendationPagingSource> = { emptyList() }
    val application get() = harness.application

    /** Maps a network manga to its library row: the row with its title, else a new id. */
    val networkToLocal = mockk<NetworkToLocalManga>()

    fun install(): RecommendationSearchHelper {
        harness.install()
        harness.serve(preferences)
        harness.serve(mockk<GetLibraryManga> { coEvery { await() } answers { library } })
        harness.serve(
            mockk<GetTracks> {
                coEvery { await() } answers { tracks }
                coEvery { await(any<Long>()) } answers { tracks }
            },
        )
        coEvery { networkToLocal(any<Manga>()) } answers {
            val incoming = firstArg<Manga>()
            library.firstOrNull { it.manga.ogTitle == incoming.ogTitle }?.manga ?: incoming.copy(id = 99L)
        }
        harness.serve(networkToLocal)
        every { sourceManager.getOrStub(any()) } returns mockk<Source>()
        harness.serve(sourceManager)
        mockkObject(RecommendationPagingSource.Companion)
        every { RecommendationPagingSource.createSources(any(), any()) } answers { sources() }
        return RecommendationSearchHelper(harness.application)
    }

    fun uninstall() = harness.uninstall()
}

/** Runs a search over one manga to its end; [errors] receives anything that escapes the search. */
internal fun RecommendationSearchHelper.finish(errors: MutableList<Throwable> = mutableListOf()): SearchStatus {
    val scope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, error -> errors += error })
    val job = checkNotNull(runSearch(scope, listOf(sourceManga())))
    return runBlocking {
        withTimeout(FAKE_WAIT_MS) {
            job.join()
            status.first()
        }
    }
}
