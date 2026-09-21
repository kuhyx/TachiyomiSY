package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.TrackerManager
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AnilistSearchTest {

    private val server = FakeServer()
    private lateinit var anilist: Anilist

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        anilist = Anilist(TrackerManager.ANILIST)
        anilist.saveCredentials("777", "tok")
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun searchGoesThroughApi() = runSuspend {
        server.enqueue(200, fixture("anilist", "search.json"))
        anilist.search("solo").size shouldBe 4
    }

    @Test
    fun searchByIdGoesThroughApi() = runSuspend {
        server.enqueue(200, fixture("anilist", "search_by_id.json"))
        anilist.searchById("105").title shouldBe "Taiwan Manhua"
    }

    @Test
    fun metadataGoesThroughApi() = runSuspend {
        server.enqueue(200, fixture("anilist", "metadata.json"))
        checkNotNull(anilist.getMangaMetadata(domainTrack(TrackerManager.ANILIST, remoteId = 101L))).title shouldBe
            "Solo Leveling"
    }
}
