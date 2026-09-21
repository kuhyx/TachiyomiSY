package eu.kanade.tachiyomi.data.track.hikka.dto

import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.hikka.Hikka
import eu.kanade.tachiyomi.data.track.hikka.stringToNumber
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class HKReadTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun decode(name: String): HKRead =
        json.decodeFromString(fixture("eu/kanade/tachiyomi/data/track/hikka/$name"))

    @Test
    fun readWithContentFillsTheManga() {
        val read = decode("read.json")
        read.reference shouldBe "read-ref"
        read.note shouldBe null
        read.updated shouldBe 1_700_000_100L
        read.created shouldBe 1_700_000_000L
        read.volumes shouldBe 2
        read.rereads shouldBe 0
        read.endDate shouldBe null

        val track = read.toTrack(10L)
        track.title shouldBe "English Title"
        track.remoteId shouldBe stringToNumber("test-manga-abc123")
        track.libraryId shouldBe stringToNumber("test-manga-abc123")
        track.totalChapters shouldBe 42L
        track.trackingUrl shouldBe "https://hikka.io/manga/test-manga-abc123"
        track.lastChapterRead shouldBe 12.0
        track.score shouldBe 8.0
        track.status shouldBe Hikka.READING
        track.startedReadingDate shouldBe 1_690_000_000_000L
        track.finishedReadingDate shouldBe 0L
    }

    @Test
    fun noContentKeepsDefaults() {
        val read = decode("read_no_content.json")
        read.content shouldBe null
        read.note shouldBe "n"

        val track = read.toTrack(10L)
        track.title shouldBe ""
        track.remoteId shouldBe 0L
        track.libraryId shouldBe null
        track.totalChapters shouldBe 0L
        track.trackingUrl shouldBe ""
        track.status shouldBe Hikka.COMPLETED
        track.startedReadingDate shouldBe 0L
        track.finishedReadingDate shouldBe 1_695_000_000_000L
    }

    @Test
    fun titleFallsBackToOriginal() {
        val content = checkNotNull(decode("read.json").content)
        decode("read.json").copy(content = content.copy(titleUa = "UA")).toTrack(1L).title shouldBe "UA"
        val bare = content.copy(titleEn = null, chapters = null)
        val track = decode("read.json").copy(content = bare).toTrack(1L)
        track.title shouldBe "Original Title"
        track.totalChapters shouldBe 0L
    }

    @Test
    fun serializesBackToJson() {
        val read = decode("read.json")
        json.decodeFromString<HKRead>(json.encodeToString(read)) shouldBe read
    }
}
