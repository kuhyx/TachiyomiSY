package eu.kanade.tachiyomi.data.track.hikka.dto

import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.hikka.Hikka
import eu.kanade.tachiyomi.data.track.hikka.stringToNumber
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class HKMangaTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun decode(name: String): HKManga =
        json.decodeFromString(fixture("eu/kanade/tachiyomi/data/track/hikka/$name"))

    @Test
    fun fullMangaBecomesATrack() {
        val manga = decode("manga.json")
        manga.dataType shouldBe "manga"
        manga.titleOriginal shouldBe "Original Title"
        manga.volumes shouldBe 5
        manga.translatedUa shouldBe true
        manga.year shouldBe 2015
        manga.scoredBy shouldBe 100

        val track = manga.toTrack(10L)
        track.trackerId shouldBe 10L
        track.remoteId shouldBe stringToNumber("test-manga-abc123")
        track.title shouldBe "Назва"
        track.totalChapters shouldBe 42L
        track.coverUrl shouldBe "https://cdn.hikka.io/manga.jpg"
        track.trackingUrl shouldBe "https://hikka.io/manga/test-manga-abc123"
        track.publishingStatus shouldBe "finished"
        track.publishingType shouldBe "one shot"
        track.startDate shouldBe "2015-01-01"
        // The user's own read entry overrides score and progress.
        track.status shouldBe Hikka.ON_HOLD
        track.lastChapterRead shouldBe 7.0
        track.score shouldBe 9.0
        track.startedReadingDate shouldBe 1_690_000_000_000L
        track.finishedReadingDate shouldBe 1_695_000_000_000L
    }

    @Test
    fun minimalMangaUsesDefaults() {
        val manga = decode("manga_minimal.json")
        manga.titleUa shouldBe null
        manga.titleEn shouldBe null
        manga.chapters shouldBe null
        manga.mediaType shouldBe null
        manga.startDate shouldBe null
        manga.read shouldBe emptyList()

        val track = manga.toTrack(10L)
        track.title shouldBe "Only Original"
        track.totalChapters shouldBe 0L
        track.publishingType shouldBe ""
        track.startDate shouldBe ""
        track.status shouldBe 0L
        track.lastChapterRead shouldBe 0.0
        track.score shouldBe 0.0
    }

    @Test
    fun englishTitleBeatsOriginal() {
        val page = json.decodeFromString<HKMangaPagination>(
            fixture("eu/kanade/tachiyomi/data/track/hikka/manga_search.json"),
        )
        val first = page.list[0].toTrack(10L)
        first.title shouldBe "First EN"
        first.startDate shouldBe ""
        first.publishingType shouldBe "manga"
        first.score shouldBe 7.0

        val second = page.list[1]
        second.read shouldBe null
        second.toTrack(10L).title shouldBe "Second"
        second.toTrack(10L).publishingType shouldBe "light novel"
    }

    @Test
    fun readEntryWithoutDatesIsZero() {
        val read = HKRead(
            reference = "r",
            note = null,
            updated = 1L,
            created = 1L,
            status = "planned",
            chapters = 3,
            volumes = 0,
            rereads = 0,
            score = 5,
        )
        val manga = decode("manga_minimal.json").copy(read = listOf(read))
        val track = manga.toTrack(10L)
        track.status shouldBe Hikka.PLAN_TO_READ
        track.lastChapterRead shouldBe 3.0
        track.startedReadingDate shouldBe 0L
        track.finishedReadingDate shouldBe 0L
    }

    @Test
    fun serializesBackToJson() {
        val manga = decode("manga.json")
        json.decodeFromString<HKManga>(json.encodeToString(manga)) shouldBe manga
        manga.copy(slug = "x").hashCode() shouldBe manga.copy(slug = "x").hashCode()
    }

    @Test
    fun constructorDefaultsMatchJson() {
        val built = HKManga(
            dataType = "manga",
            titleOriginal = "Only Original",
            mediaType = null,
            translatedUa = false,
            status = "ongoing",
            image = "https://cdn.hikka.io/min.jpg",
            scoredBy = 0,
            score = 0.0,
            slug = "minimal-slug",
        )
        built shouldBe decode("manga_minimal.json")
        built.toString() shouldBe decode("manga_minimal.json").toString()
    }
}
