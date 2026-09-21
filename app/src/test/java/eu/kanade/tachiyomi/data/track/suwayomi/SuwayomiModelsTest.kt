package eu.kanade.tachiyomi.data.track.suwayomi

import eu.kanade.tachiyomi.data.track.fixture
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class SuwayomiModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun load(name: String): String = fixture("eu/kanade/tachiyomi/data/track/suwayomi/$name")

    @Test
    fun mangaDecodes() {
        val manga = json.decodeFromString<GetMangaResult>(load("manga.json")).data.entry
        manga.artist shouldBe "Artist"
        manga.author shouldBe "Author"
        manga.description shouldBe "A description"
        manga.id shouldBe 42
        manga.status shouldBe MangaStatus.ONGOING
        manga.thumbnailUrl shouldBe "api/v1/manga/42/thumbnail"
        manga.title shouldBe "Suwayomi Manga"
        manga.url shouldBe "/manga/42"
        manga.genre shouldBe listOf("action", "drama")
        manga.inLibraryAt shouldBe 1_700_000_000L
        manga.chapters shouldBe MangaFragment.Chapters(totalCount = 20)
        manga.chapters.totalCount shouldBe 20
        manga.latestUploadedChapter shouldBe MangaFragment.LatestUploadedChapter(uploadDate = 1_700_000_100L)
        manga.latestUploadedChapter?.uploadDate shouldBe 1_700_000_100L
        manga.latestFetchedChapter shouldBe MangaFragment.LatestFetchedChapter(fetchedAt = 1_700_000_200L)
        manga.latestFetchedChapter?.fetchedAt shouldBe 1_700_000_200L
        manga.latestReadChapter shouldBe
            MangaFragment.LatestReadChapter(lastReadAt = 1_700_000_300L, chapterNumber = 12.5)
        manga.latestReadChapter?.lastReadAt shouldBe 1_700_000_300L
        manga.latestReadChapter?.chapterNumber shouldBe 12.5
        manga.unreadCount shouldBe 7
        manga.downloadCount shouldBe 3
        val encoded = json.encodeToString(GetMangaResult(GetMangaData(manga)))
        json.decodeFromString<GetMangaResult>(encoded).data.entry shouldBe manga
    }

    @Test
    fun minimalMangaDecodes() {
        val manga = json.decodeFromString<GetMangaResult>(load("manga_minimal.json")).data.entry
        manga.artist shouldBe null
        manga.description shouldBe null
        manga.thumbnailUrl shouldBe null
        manga.latestUploadedChapter shouldBe null
        manga.latestFetchedChapter shouldBe null
        manga.latestReadChapter shouldBe null
        manga.status shouldBe MangaStatus.UNKNOWN
        val built = MangaFragment(
            artist = null,
            author = null,
            description = null,
            id = 43,
            status = MangaStatus.UNKNOWN,
            thumbnailUrl = null,
            title = "Untouched",
            url = "/manga/43",
            genre = emptyList(),
            inLibraryAt = 0L,
            chapters = MangaFragment.Chapters(5),
            latestUploadedChapter = null,
            latestFetchedChapter = null,
            latestReadChapter = null,
            unreadCount = 5,
            downloadCount = 0,
        )
        built shouldBe manga
        built.toString() shouldBe manga.toString()
    }

    @Test
    fun statusesCarryTheirRawValue() {
        MangaStatus.entries.map { it.rawValue } shouldBe MangaStatus.entries.map { it.name }
        MangaStatus.valueOf("ON_HIATUS").rawValue shouldBe "ON_HIATUS"
        MangaStatus.entries.size shouldBe 7
    }

    @Test
    fun unreadChaptersDecode() {
        val result = json.decodeFromString<GetMangaUnreadChaptersResult>(load("unread_chapters.json"))
        val nodes = result.data.entry.nodes
        nodes.size shouldBe 3
        nodes[1] shouldBe GetMangaUnreadChaptersNode(id = 102, chapterNumber = 12.5)
        nodes[1].id shouldBe 102
        nodes[1].chapterNumber shouldBe 12.5
        val built = GetMangaUnreadChaptersResult(GetMangaUnreadChaptersData(GetMangaUnreadChaptersEntry(nodes)))
        built shouldBe result
        json.encodeToString(built) shouldBe load("unread_chapters.json").replace(Regex("\\s"), "")
    }
}
