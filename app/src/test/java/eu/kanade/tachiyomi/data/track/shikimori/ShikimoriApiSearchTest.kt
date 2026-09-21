package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ShikimoriApiSearchTest {

    private val server = FakeServer()
    private lateinit var api: ShikimoriApi

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        api = ShikimoriApi(TrackerManager.SHIKIMORI, server.client, mockk())
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun searchMapsResults() = runSuspend {
        server.enqueue(200, fixture("shikimori", "search.json"))
        val results = api.search("berserk")
        results.map { it.remoteId } shouldBe listOf(2L, 3L, 4L)
        results[0].trackerId shouldBe TrackerManager.SHIKIMORI
        server.lastRequest.url.toString() shouldBe ShikimoriApi.GRAPHQL_API_URL
        server.bodies.single() shouldContain """"query":"berserk""""
    }

    @Test
    fun metadataMapsRoles() = runSuspend {
        server.enqueue(200, fixture("shikimori", "metadata.json"))
        api.getMangaMetadata(domainTrack(TrackerManager.SHIKIMORI, remoteId = 2L)) shouldBe TrackMangaMetadata(
            remoteId = 2L,
            title = "Berserk",
            thumbnailUrl = "https://img/2o.jpg",
            description = "Guts.",
            authors = "Kentarou Miura, Only Writer",
            artists = "Kentarou Miura, Only Artist",
        )
        server.lastRequest.url.toString() shouldBe "https://shikimori.one/api/graphql"
        server.bodies.single() shouldContain """"ids":"2""""
    }

    @Test
    fun metadataWithoutRolesOrEntry() = runSuspend {
        server.enqueue(200, fixture("shikimori", "metadata_bare.json"))
        val bare = api.getMangaMetadata(domainTrack(TrackerManager.SHIKIMORI, remoteId = 3L))
        bare.authors shouldBe null
        bare.artists shouldBe null
        bare.description shouldBe ""
        server.enqueue(200, fixture("shikimori", "metadata_empty.json"))
        shouldThrow<NoSuchElementException> { api.getMangaMetadata(domainTrack(TrackerManager.SHIKIMORI)) }
    }

    @Test
    fun authUrlAndRefreshRequest() {
        ShikimoriApi.authUrl().toString() shouldBe "https://shikimori.io/oauth/authorize?" +
            "client_id=PB9dq8DzI405s7wdtwTdirYqHiyVMh--djnP7lBUqSA&redirect_uri=mihon%3A%2F%2Fshikimori-auth&" +
            "response_type=code"
        ShikimoriApi.refreshTokenRequest("tok").url.toString() shouldBe "https://shikimori.io/oauth/token"
    }
}
