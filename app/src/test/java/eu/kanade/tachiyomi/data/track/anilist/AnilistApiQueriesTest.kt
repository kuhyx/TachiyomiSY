package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
internal class AnilistApiQueriesTest {

    private val server = FakeServer()
    private lateinit var api: AnilistApi

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        api = AnilistApi(server.client, mockk())
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun searchMapsEveryMedia() = runSuspend {
        server.enqueue(200, fixture("anilist", "search.json"))
        val results = api.search("solo")
        results.map { it.remoteId } shouldBe listOf(101L, 102L, 103L, 104L)
        val first = results[0]
        first.title shouldBe "Solo Leveling"
        first.summary shouldBe "Hunters & gates\n"
        first.publishingType shouldBe "Manhwa"
        first.publishingStatus shouldBe "FINISHED"
        first.totalChapters shouldBe 179L
        first.score shouldBe 85.0
        first.coverUrl shouldBe "https://img/101.jpg"
        first.trackingUrl shouldBe "https://anilist.co/manga/101"
        first.startDate shouldBe "2018-03-04"
        first.authors shouldBe listOf("Preferred Name", "Writer Full")
        first.artists shouldBe listOf("Preferred Name", "画家")
        server.bodies.single() shouldContain "query Search"
        server.bodies.single() shouldContain """"query":"solo""""
    }

    @Test
    fun searchDefaultsMissingFields() = runSuspend {
        server.enqueue(200, fixture("anilist", "search.json"))
        val results = api.search("solo")
        val oneShot = results[1]
        oneShot.publishingType shouldBe "ONE-SHOT"
        oneShot.publishingStatus shouldBe ""
        oneShot.totalChapters shouldBe 0L
        oneShot.score shouldBe -1.0
        oneShot.summary shouldBe ""
        oneShot.startDate shouldBe ""
        results[2].publishingType shouldBe "Manhua"
        results[2].startDate shouldBe ""
        results[3].publishingType shouldBe "Manga"
    }

    @Test
    fun searchByIdMapsSingleMedia() = runSuspend {
        server.enqueue(200, fixture("anilist", "search_by_id.json"))
        val result = api.searchById("105")
        result.remoteId shouldBe 105L
        result.publishingType shouldBe "Manhua"
        result.startDate shouldBe "2022-02-02"
        server.bodies.single() shouldContain """"mangaId":"105""""
    }

    @Test
    fun currentUserReadsViewer() = runSuspend {
        server.enqueue(200, fixture("anilist", "current_user.json"))
        val user = api.getCurrentUser()
        user.id shouldBe 777
        user.name shouldBe "kuhy"
        user.mediaListOptions.scoreFormat shouldBe "POINT_100"
        server.bodies.single() shouldContain "query User"
    }

    @Test
    fun createDateSplitsEpochMillis() {
        val zero = api.createDate(0L)
        zero["year"] shouldBe JsonNull
        zero["month"] shouldBe JsonNull
        zero["day"] shouldBe JsonNull
        val date = LocalDate.of(2024, 2, 29).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val built = api.createDate(date)
        built["year"] shouldBe JsonPrimitive(2024)
        checkNotNull(built["month"]).jsonPrimitive.content shouldBe "2"
        checkNotNull(built["day"]).jsonPrimitive.content shouldBe "29"
    }

    @Test
    fun metadataMapsStaffByRole() = runSuspend {
        server.enqueue(200, fixture("anilist", "metadata.json"))
        api.getMangaMetadata(domainTrack(TrackerManager.ANILIST, remoteId = 101L)) shouldBe TrackMangaMetadata(
            remoteId = 101L,
            title = "Solo Leveling",
            thumbnailUrl = "https://img/101.jpg",
            description = "Hunters & gates",
            authors = "Both, Writer",
            artists = "Both, Artist",
        )
        server.bodies.single() shouldContain """"mangaId":101"""
    }

    @Test
    fun metadataWithoutStaff() = runSuspend {
        server.enqueue(200, fixture("anilist", "metadata_bare.json"))
        val bare = api.getMangaMetadata(domainTrack(TrackerManager.ANILIST, remoteId = 102L))
        bare.description shouldBe null
        bare.authors shouldBe null
        bare.artists shouldBe null
        server.enqueue(200, fixture("anilist", "metadata_blank.json"))
        api.getMangaMetadata(domainTrack(TrackerManager.ANILIST, remoteId = 103L)).description shouldBe null
    }

    @Test
    fun authUrlCarriesClientId() {
        AnilistApi.authUrl().toString() shouldBe
            "https://anilist.co/api/v2/oauth/authorize?client_id=16329&response_type=token"
    }
}
