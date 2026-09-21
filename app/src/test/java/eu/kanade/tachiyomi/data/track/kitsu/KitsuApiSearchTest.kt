package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuMangaMetadata
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class KitsuApiSearchTest {

    private val server = FakeServer()
    private lateinit var api: KitsuApi

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        api = KitsuApi(server.client, mockk())
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun searchGoesViaAlgoliaKey() = runSuspend {
        server.enqueue(200, fixture("kitsu", "algolia_key.json"))
        server.enqueue(200, fixture("kitsu", "algolia_search.json"))
        val results = api.search("one piece")
        results.map { it.remoteId } shouldBe listOf(1L, 3L)
        server.requests[0].url.toString() shouldBe "https://kitsu.app/api/edge/algolia-keys/media/"
        val algolia = server.requests[1]
        algolia.url.toString() shouldBe "https://awqo5j657s-dsn.algolia.net/1/indexes/production_media/query/"
        algolia.header("X-Algolia-API-Key") shouldBe "algolia-key"
        algolia.header("X-Algolia-Application-Id") shouldBe "AWQO5J657S"
        server.bodies[1] shouldContain "query=one+piece&facetFilters"
    }

    @Test
    fun metadataMapsStaffByRole() = runSuspend {
        server.enqueue(200, fixture("kitsu", "metadata.json"))
        val metadata = api.getMangaMetadata(domainTrack(TrackerManager.KITSU, remoteId = 42L))
        metadata shouldBe TrackMangaMetadata(
            remoteId = 42L,
            title = "One Piece",
            thumbnailUrl = "https://img/op.jpg",
            description = "Pirates & treasure",
            authors = "Eiichiro Oda, Writer",
            artists = "Eiichiro Oda, Artist",
        )
        server.lastRequest.url.toString() shouldBe "https://kitsu.app/api/graphql"
        server.lastRequest.header("Accept-Language") shouldBe "en"
        server.bodies.single() shouldContain """"libraryId":42"""
    }

    @Test
    fun metadataWithoutStaff() = runSuspend {
        server.enqueue(200, fixture("kitsu", "metadata_empty.json"))
        val metadata = api.getMangaMetadata(domainTrack(TrackerManager.KITSU, remoteId = 7L))
        metadata.description shouldBe null
        metadata.authors shouldBe null
        metadata.artists shouldBe null
    }

    @Test
    fun blankDescriptionBecomesNull() = runSuspend {
        server.enqueue(200, fixture("kitsu", "metadata_blank_description.json"))
        api.getMangaMetadata(domainTrack(TrackerManager.KITSU, remoteId = 8L)).description shouldBe null
    }

    @Test
    fun metadataDtoRoundTrips() {
        val decoded = TrackerHarness.json.decodeFromString<KitsuMangaMetadata>(fixture("kitsu", "metadata.json"))
        exerciseDto(decoded)
        val media = decoded.data.findLibraryEntryById.media
        media.id shouldBe "42"
        media.staff.nodes.size shouldBe 4
        TrackerHarness.json.decodeFromString<KitsuMangaMetadata>(TrackerHarness.json.encodeToString(decoded)) shouldBe
            decoded
    }
}
