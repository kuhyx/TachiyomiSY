package eu.kanade.presentation.more.settings.screen

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.StubSource
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf

/** The one installed source an enhanced tracker in these tests accepts. */
internal class InstalledSource : StubSource()

/** A logged-out tracker stub named [trackerName]; [T] is its concrete class as [TrackerManager] declares it. */
internal inline fun <reified T : Tracker> stubTracker(trackerName: String, loggedIn: Boolean = false): T = mockk {
    every { name } returns trackerName
    every { getLogo() } returns R.drawable.ic_tachi
    every { isLoggedIn } returns loggedIn
    every { isLoggedInFlow } returns flowOf(loggedIn)
    every { getDisplayUsername() } returns ""
    every { getUsername() } returns ""
    every { getPassword() } returns ""
    every { logout() } just runs
}

/** An enhanced tracker accepting [accepted] source classes. */
internal fun enhancedTracker(trackerName: String, accepted: List<String>): Tracker {
    val tracker = mockk<Tracker>(moreInterfaces = arrayOf(EnhancedTracker::class))
    val enhanced = tracker as EnhancedTracker
    every { tracker.name } returns trackerName
    every { tracker.getLogo() } returns R.drawable.ic_tachi
    every { tracker.isLoggedIn } returns false
    every { tracker.isLoggedInFlow } returns flowOf(false)
    every { tracker.getDisplayUsername() } returns ""
    every { tracker.logout() } just runs
    every { enhanced.getAcceptedSources() } returns accepted
    every { enhanced.loginNoop() } just runs
    return tracker
}

internal fun stubTrackerManager(enhanced: List<Tracker>): TrackerManager = mockk {
    every { mangaBaka } returns stubTracker("MangaBaka")
    every { myAnimeList } returns stubTracker("MyAnimeList")
    every { aniList } returns stubTracker("AniList")
    every { kitsu } returns stubTracker("Kitsu")
    every { mangaUpdates } returns stubTracker("MangaUpdates")
    every { shikimori } returns stubTracker("Shikimori")
    every { bangumi } returns stubTracker("Bangumi")
    every { hikka } returns stubTracker("Hikka")
    every { trackers } returns enhanced + listOf(stubTracker<Tracker>("Plain"))
}
