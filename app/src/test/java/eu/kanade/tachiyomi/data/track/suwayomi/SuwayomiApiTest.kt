package eu.kanade.tachiyomi.data.track.suwayomi

import eu.kanade.tachiyomi.data.track.bodyText
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SuwayomiApiTest {

    private val harness = SuwayomiHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun trackSearchMapsTheManga() = runTest {
        harness.enqueue("manga.json")
        val track = harness.api.getTrackSearch(42L)
        track.trackerId shouldBe 9L
        track.remoteId shouldBe 42L
        track.title shouldBe "Suwayomi Manga"
        track.coverUrl shouldBe "http://suwayomi.local/api/v1/manga/42/thumbnail"
        track.summary shouldBe "A description"
        track.trackingUrl shouldBe "http://suwayomi.local/manga/42"
        track.totalChapters shouldBe 20L
        track.publishingStatus shouldBe "ONGOING"
        track.lastChapterRead shouldBe 12.5
        track.status shouldBe Suwayomi.READING

        val request = harness.takeRequest()
        request.method shouldBe "POST"
        request.url.encodedPath shouldBe "/api/graphql"
        val body = request.bodyText()
        body shouldContain "query GetManga"
        body shouldContain "fragment MangaFragment on MangaType"
        body shouldContain """"variables":{"mangaId":42}"""
    }

    @Test
    fun trackSearchDefaults() = runTest {
        harness.enqueue("manga_minimal.json")
        val track = harness.api.getTrackSearch(43L)
        track.coverUrl shouldBe "http://suwayomi.local/null"
        track.summary shouldBe ""
        track.lastChapterRead shouldBe 0.0
        track.status shouldBe Suwayomi.UNREAD
        track.publishingStatus shouldBe "UNKNOWN"

        harness.enqueueRaw(
            """{"data":{"manga":{"id":1,"status":"COMPLETED","title":"t","url":"u","genre":[],"inLibraryAt":0,""" +
                """"chapters":{"totalCount":3},"unreadCount":0,"downloadCount":0}}}""",
        )
        harness.api.getTrackSearch(1L).status shouldBe Suwayomi.COMPLETED

        harness.enqueueRaw("", code = 500)
        shouldThrow<HttpException> { harness.api.getTrackSearch(1L) }.code shouldBe 500
    }

    @Test
    fun updateMarksOlderChaptersRead() = runTest {
        harness.enqueue("unread_chapters.json")
        harness.enqueueRaw("""{"data":{"updateChapters":{"__typename":"UpdateChaptersPayload"}}}""")
        harness.enqueueRaw("""{"data":{"trackProgress":{"__typename":"TrackProgressPayload"}}}""")
        harness.enqueue("manga.json")
        val track = dbTrack(trackerId = 9L, remoteId = 42L, lastChapterRead = 12.5)
        harness.api.updateProgress(track).title shouldBe "Suwayomi Manga"

        harness.takeRequest().bodyText() shouldContain "query GetMangaUnreadChapters"
        val mark = harness.takeRequest().bodyText()
        mark shouldContain "mutation MarkChaptersRead"
        mark shouldNotContain "deleteDownloadedChapters"
        mark shouldContain """"variables":{"chapters":[101,102]}"""
        val progress = harness.takeRequest().bodyText()
        progress shouldContain "mutation TrackManga"
        progress shouldContain """"variables":{"mangaId":42}"""
        harness.takeRequest().bodyText() shouldContain "query GetManga("
    }

    @Test
    fun updateCanDeleteDownloads() = runTest {
        harness.enqueue("unread_chapters.json")
        harness.enqueueRaw("{}")
        harness.enqueueRaw("{}")
        harness.enqueue("manga.json")
        val track = dbTrack(trackerId = 9L, remoteId = 42L, lastChapterRead = 5.0)
        harness.api.updateProgress(track, deleteDownloadsOnServer = true)
        harness.takeRequest()
        val mark = harness.takeRequest().bodyText()
        mark shouldContain "deleteDownloadedChapters"
        mark shouldContain """"variables":{"chapters":[]}"""
    }

    @Test
    fun sourcePreferencesAndPayload() {
        harness.api.sourcePreferences() shouldBe harness.preferences
        graphQlPayload("query X") { put("a", 1) }.toString() shouldBe """{"query":"query X","variables":{"a":1}}"""
        buildJsonObject { }.toString() shouldBe "{}"
    }
}
