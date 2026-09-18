package exh.source

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.EchoHttpSource
import eu.kanade.tachiyomi.source.online.RecordingHttpSource
import eu.kanade.tachiyomi.source.online.SourceHarness
import eu.kanade.tachiyomi.source.online.StubDelegatedSource
import eu.kanade.tachiyomi.source.online.invokeDeclared
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rx.Observable

/** Details, chapters and url forwards of [DelegatedHttpSourceManga]. */
internal class DelegatedHttpSourceMangaTest {
    private val harness = SourceHarness()
    private val inner = EchoHttpSource()
    private val delegated = StubDelegatedSource(delegate = inner)
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
            delegated.getMangaUpdate(manga = manga, chapters = emptyList(), fetchDetails = true, fetchChapters = true)
        update.manga.title shouldBe "manga body"
        update.chapters.single().name shouldBe "manga body"
        harness.server.requests.size shouldBe 2
    }

    @Test
    fun rxDetailsAndChaptersForward() {
        val details = delegated.invokeDeclared(DelegatedHttpSourceManga::class, "fetchMangaDetails", listOf(manga))
        ((details as Observable<*>).toBlocking().single() as SManga).title shouldBe "manga body"
        val chapters = delegated.invokeDeclared(DelegatedHttpSourceManga::class, "fetchChapterList", listOf(manga))
        ((chapters as Observable<*>).toBlocking().single() as List<*>).size shouldBe 1
    }

    @Test
    fun mangaDetailsRequestForwards() {
        val request = delegated.invokeDeclared(DelegatedHttpSourceManga::class, "mangaDetailsRequest", listOf(manga))
        (request as Request).url.toString() shouldBe "https://bare.example/manga/1"
    }

    @Test
    fun urlsForward() {
        delegated.getMangaUrl(manga) shouldBe "https://bare.example/manga/1"
        delegated.getChapterUrl(chapter) shouldBe "https://bare.example/chapter/1"
    }

    @Test
    fun chapterHookAndFiltersForward() {
        val recording = RecordingHttpSource()
        val wrapper = StubDelegatedSource(delegate = recording)
        wrapper.invokeDeclared(DelegatedHttpSourceManga::class, "prepareNewChapter", listOf(chapter, manga))
        recording.prepared shouldBe listOf(chapter to manga)
        (wrapper.getFilterList() === recording.filters) shouldBe true
    }

    @Test
    fun forwardsCheckCompatibility() = runTest {
        val mismatched = StubDelegatedSource(delegate = inner, versionId = 2)
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.getMangaUpdate(manga = manga, chapters = emptyList(), fetchDetails = false, fetchChapters = true)
        }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> { mismatched.getMangaUrl(manga) }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> { mismatched.getChapterUrl(chapter) }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSourceManga::class, "fetchMangaDetails", listOf(manga))
        }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSourceManga::class, "fetchChapterList", listOf(manga))
        }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSourceManga::class, "mangaDetailsRequest", listOf(manga))
        }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSourceManga::class, "prepareNewChapter", listOf(chapter, manga))
        }
    }
}
