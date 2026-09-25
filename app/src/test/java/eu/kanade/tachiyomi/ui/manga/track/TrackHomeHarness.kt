package eu.kanade.tachiyomi.ui.manga.track

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.ui.manga.NoCustomInfo
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import exh.metadata.metadata.MangaDexSearchMetadata
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track

/** Koin and mocks for the track dialog's screens and [TrackInfoDialogHomeModel]. */
internal class TrackHomeHarness {
    val app: Application = ApplicationProvider.getApplicationContext()
    val store: FlowPreferenceStore = FlowPreferenceStore()
    val trackPreferences: TrackPreferences = TrackPreferences(store)
    val tracks: MutableStateFlow<List<Track>> = MutableStateFlow(emptyList())
    val getTracks: GetTracks = mockk { coEvery { subscribe(any<Long>()) } returns tracks }
    val tracker: Tracker = tracker(1L, "Plain")
    val trackerManager: TrackerManager = mockk(relaxed = true) {
        every { loggedInTrackers() } returns listOf(tracker)
        every { get(1L) } returns tracker
        every { get(99L) } returns null
    }
    val refreshTracks: RefreshTracks = mockk { coEvery { await(any()) } returns emptyList() }
    val getManga: GetManga = mockk()
    val metadataSource: MetadataSource<*, *> = mockk(relaxed = true) {
        every { metaClass } returns MangaDexSearchMetadata::class
    }
    val sourceManager: SourceManager = mockk(relaxed = true) {
        every { get(7L) } returns metadataSource
        every { get(8L) } returns null
    }
    val getFlatMetadata: GetFlatMetadataById = mockk()

    fun tracker(trackerId: Long, trackerName: String): Tracker = mockk(relaxed = true) {
        every { id } returns trackerId
        every { name } returns trackerName
    }

    fun start() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startKoin {
            modules(
                module {
                    single { getTracks }
                    single { trackerManager }
                    single { trackPreferences }
                    single { refreshTracks }
                    single { app }
                    single { getManga }
                    single { sourceManager }
                    single { getFlatMetadata }
                    single { UiPreferences(store) }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
    }

    fun stop() {
        stopKoin()
        Dispatchers.resetMain()
        clearVoyagerScopes()
    }

    /** Metadata for manga 1 carrying the given tracker ids. */
    fun metadata(anilist: String? = null, kitsu: String? = null, mal: String? = null, mangaUpdates: String? = null) =
        MangaDexSearchMetadata().apply {
            mangaId = 1L
            anilistId = anilist
            kitsuId = kitsu
            myAnimeListId = mal
            mangaUpdatesId = mangaUpdates
        }.flatten()
}
