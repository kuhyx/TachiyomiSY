package exh.source

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.EchoHttpSource
import eu.kanade.tachiyomi.source.online.RecordingHttpSource
import eu.kanade.tachiyomi.source.online.SourceHarness
import eu.kanade.tachiyomi.source.online.invokeDeclared
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rx.Observable

/** Details, chapters and url forwards of [EnhancedHttpSourceManga] to whichever source is active. */
internal class EnhancedHttpSourceMangaTest {
    private val harness = SourceHarness()
    private val original = RecordingHttpSource()
    private val enhanced = EchoHttpSource()
    private val source = EnhancedHttpSource(original, enhanced)
    private val manga = SManga(url = "/manga/1", title = "stored")
    private val chapter = SChapter(name = "c", url = "/chapter/1")

    @BeforeEach
    fun setUp() {
        harness.install()
        harness.server.body = "manga body"
    }

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun mangaUpdateForwards() = runTest {
        val update =
            source.getMangaUpdate(manga = manga, chapters = emptyList(), fetchDetails = true, fetchChapters = true)
        update.manga.title shouldBe "manga body"
        update.chapters.single().name shouldBe "manga body"
        harness.delegateSources = false
        shouldThrow<UnsupportedOperationException> {
            source.getMangaUpdate(manga = manga, chapters = emptyList(), fetchDetails = true, fetchChapters = false)
        }
    }

    @Test
    fun rxDetailsAndChaptersForward() {
        val details = source.invokeDeclared(EnhancedHttpSourceManga::class, "fetchMangaDetails", listOf(manga))
        ((details as Observable<*>).toBlocking().single() as SManga).title shouldBe "manga body"
        val chapters = source.invokeDeclared(EnhancedHttpSourceManga::class, "fetchChapterList", listOf(manga))
        ((chapters as Observable<*>).toBlocking().single() as List<*>).size shouldBe 1
    }

    @Test
    fun mangaDetailsRequestForwards() {
        val request = source.invokeDeclared(EnhancedHttpSourceManga::class, "mangaDetailsRequest", listOf(manga))
        (request as Request).url.toString() shouldBe "https://bare.example/manga/1"
    }

    @Test
    fun urlsForward() {
        source.getMangaUrl(manga) shouldBe "https://bare.example/manga/1"
        source.getChapterUrl(chapter) shouldBe "https://bare.example/chapter/1"
    }

    @Test
    fun chapterHookForwardsToActive() {
        source.invokeDeclared(EnhancedHttpSourceManga::class, "prepareNewChapter", listOf(chapter, manga))
        original.prepared shouldBe emptyList()
        harness.delegateSources = false
        source.invokeDeclared(EnhancedHttpSourceManga::class, "prepareNewChapter", listOf(chapter, manga))
        original.prepared shouldBe listOf(chapter to manga)
    }

    @Test
    fun filtersForwardToActiveSource() {
        (source.getFilterList() === original.filters) shouldBe false
        harness.delegateSources = false
        (source.getFilterList() === original.filters) shouldBe true
    }
}
