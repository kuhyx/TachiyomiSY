package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MyAnimeListApiListTest {

    private val server = FakeServer()
    private lateinit var api: MyAnimeListApi

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        api = MyAnimeListApi(TrackerManager.MYANIMELIST, server.client, mockk())
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun listItemFoundFillsTrack() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "list_item.json"))
        val track = dbTrack(TrackerManager.MYANIMELIST, remoteId = 2L)
        checkNotNull(api.findListItem(track))
        track.totalChapters shouldBe 380L
        track.lastChapterRead shouldBe 42.0
        track.status shouldBe MyAnimeList.READING
        server.lastRequest.url.toString() shouldBe "https://api.myanimelist.net/v2/manga/2?fields=" +
            "num_chapters%2Cmy_list_status%7Bstart_date%2Cfinish_date%7D"
    }

    @Test
    fun listItemAbsentIsNull() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "list_item_absent.json"))
        val track = dbTrack(TrackerManager.MYANIMELIST, remoteId = 2L)
        api.findListItem(track).shouldBeNull()
        track.totalChapters shouldBe 380L
    }

    @Test
    fun listSearchFollowsPaging() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "mangalist_page1.json"))
        server.enqueue(200, fixture("myanimelist", "mangalist_page2.json"))
        val matches = api.findListItems("berserk")
        matches.map { it.remoteId } shouldBe listOf(2L, 6L)
        server.requests[0].url.queryParameter("offset").shouldBeNull()
        server.requests[0].url.queryParameter("limit") shouldBe "250"
        server.requests[1].url.queryParameter("offset") shouldBe "250"
        server.enqueue(200, fixture("myanimelist", "search.json"))
        api.findListItems("novel").map { it.remoteId } shouldBe listOf(3L)
    }

    @Test
    fun listPageWithoutOffset() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "mangalist_page2.json"))
        api.getListPage(0).paging.next shouldBe ""
        server.lastRequest.url.toString() shouldBe "https://api.myanimelist.net/v2/users/@me/mangalist?fields=" +
            "id%2Ctitle%2Csynopsis%2Cnum_chapters%2Cmean%2Cmain_picture%2Cstatus%2Cmedia_type%2Cstart_date%2C" +
            "authors%7Bfirst_name%2Clast_name%7D&limit=250"
    }
}
