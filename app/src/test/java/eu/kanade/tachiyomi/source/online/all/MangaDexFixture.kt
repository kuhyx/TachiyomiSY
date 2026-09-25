package eu.kanade.tachiyomi.source.online.all

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.online.FakeDelegateSource
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.serveMetadataSource
import exh.md.network.MangaDexAuthInterceptor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertFlatMetadata

/** The MangaDex source over the harness: a fake extension delegate, a mocked MDList tracker and no saved manga. */
internal class MangaDexFixture(private val harness: SourceTestHarness, lang: String = "en") {
    val trackPreferences: TrackPreferences = TrackPreferences(harness.store)
    val mdList: MdList = mockk { every { id } returns TrackerManager.MDLIST }
    val interceptor: MangaDexAuthInterceptor = MangaDexAuthInterceptor(trackPreferences, mdList)
    val delegate: FakeDelegateSource = FakeDelegateSource(harness.baseUrl, lang = lang, name = "MangaDex")
    val source: MangaDex

    init {
        every { mdList.interceptor } returns interceptor
        val trackerManager = mockk<TrackerManager>()
        every { trackerManager.mdList } returns mdList
        harness.serve(trackPreferences)
        harness.serve(trackerManager)
        harness.serveMetadataSource()
        val getManga = mockk<GetManga>()
        coEvery { getManga.await(any<String>(), any()) } returns null
        harness.serve(getManga)
        val getFlat = mockk<GetFlatMetadataById>()
        coEvery { getFlat.await(any()) } returns null
        harness.serve(getFlat)
        harness.serve<InsertFlatMetadata>(mockk())
        source = MangaDex(delegate, harness.application)
    }
}
