package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListItemStatus
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALManga
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
internal class MyAnimeListApiParsingTest {

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

    private fun listStatus(
        isRereading: Boolean = false,
        startDate: String? = null,
        finishDate: String? = null,
    ) = MALListItemStatus(
        isRereading = isRereading,
        status = "on_hold",
        numChaptersRead = 42.0,
        score = 8,
        startDate = startDate,
        finishDate = finishDate,
    )

    @Test
    fun metadataMapsAuthorsAndCover() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "metadata.json"))
        api.getMangaMetadata(domainTrack(TrackerManager.MYANIMELIST, remoteId = 2L)) shouldBe TrackMangaMetadata(
            remoteId = 2L,
            title = "Berserk",
            thumbnailUrl = "https://img/2m.jpg",
            description = "Guts.",
            authors = "Kentarou Miura, Only Writer",
            artists = "Kentarou Miura, Only Artist",
        )
        server.lastRequest.url.toString() shouldBe "https://api.myanimelist.net/v2/manga/2?fields=" +
            "id%2Ctitle%2Csynopsis%2Cmain_picture%2Cauthors%7Bfirst_name%2Clast_name%7D"
    }

    @Test
    fun metadataWithoutAuthors() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "metadata_bare.json"))
        val bare = checkNotNull(api.getMangaMetadata(domainTrack(TrackerManager.MYANIMELIST, remoteId = 3L)))
        bare.thumbnailUrl shouldBe "https://img/3l.jpg"
        bare.description.shouldBeNull()
        bare.authors.shouldBeNull()
        bare.artists.shouldBeNull()
    }

    @Test
    fun mangaItemCopiesListStatus() {
        val track = dbTrack(TrackerManager.MYANIMELIST)
        api.parseMangaItem(listStatus(startDate = "2024-01-02", finishDate = "2024-03-04"), track)
        track.status shouldBe MyAnimeList.ON_HOLD
        track.lastChapterRead shouldBe 42.0
        track.score shouldBe 8.0
        track.startedReadingDate shouldBe api.parseDate("2024-01-02")
        track.finishedReadingDate shouldBe api.parseDate("2024-03-04")
    }

    @Test
    fun mangaItemRereadingWithoutDates() {
        val track = dbTrack(TrackerManager.MYANIMELIST)
        api.parseMangaItem(listStatus(isRereading = true), track)
        track.status shouldBe MyAnimeList.REREADING
        track.startedReadingDate shouldBe 0L
        track.finishedReadingDate shouldBe 0L
    }

    @Test
    fun searchItemMapsEveryField() {
        val manga = TrackerHarness.json.decodeFromString<MALManga>(fixture("myanimelist", "manga_details.json"))
        val track = api.parseSearchItem(manga)
        track.trackerId shouldBe TrackerManager.MYANIMELIST
        track.remoteId shouldBe 2L
        track.title shouldBe "Berserk"
        track.summary shouldBe "Guts."
        track.totalChapters shouldBe 380L
        track.score shouldBe 9.47
        track.coverUrl shouldBe "https://img/2l.jpg"
        track.trackingUrl shouldBe "https://myanimelist.net/manga/2"
        track.publishingStatus shouldBe "currently publishing"
        track.publishingType shouldBe "manga"
        track.startDate shouldBe "1989-08-25"
        track.authors shouldBe listOf("Kentarou Miura")
        track.artists shouldBe emptyList()
    }

    @Test
    fun searchItemWithoutOptionals() {
        val manga = MALManga(
            id = 4L,
            title = "Bare",
            numChapters = 0L,
            covers = null,
            status = "finished",
            mediaType = "one_shot",
            startDate = null,
        )
        val track = api.parseSearchItem(manga)
        track.coverUrl shouldBe ""
        track.startDate shouldBe ""
        track.publishingType shouldBe "one shot"
        track.summary shouldBe ""
        track.score shouldBe -1.0
    }

    @Test
    fun datesRoundTrip() {
        val millis = checkNotNull(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2024-01-02")).time
        api.parseDate("2024-01-02") shouldBe millis
        api.convertToIsoDate(millis) shouldBe "2024-01-02"
        api.convertToIsoDate(0L) shouldBe ""
    }
}
