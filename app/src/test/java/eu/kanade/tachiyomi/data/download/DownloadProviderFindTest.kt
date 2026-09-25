package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadProviderFindTest : ProviderTestBase() {

    @Test
    fun findSourceAndMangaDir() {
        chapterDir(mangaTitle = "Title", name = "Ch 1")
        harness.provider.findSourceDir(source)!!.name shouldBe "Source"
        harness.provider.findMangaDir(mangaTitle = "Title", source = source)!!.name shouldBe "Title"
        harness.provider.findMangaDir(mangaTitle = "Absent", source = source).shouldBeNull()
        harness.provider.findSourceDir(namedSource("Other")).shouldBeNull()
        DownloadProviderHarness(root = null, context = context).provider.findSourceDir(source).shouldBeNull()
    }

    @Test
    fun findChapterDirAnyValidName() {
        chapterDir(mangaTitle = "Title", name = "Ch 1.cbz")
        val found = harness.provider.findChapterDir(
            chapterName = "Ch 1",
            chapterScanlator = null,
            chapterUrl = "/u",
            mangaTitle = "Title",
            source = source,
        )
        found!!.name shouldBe "Ch 1.cbz"
    }

    @Test
    fun findChapterDirWithoutMangaDir() {
        harness.provider.findChapterDir(
            chapterName = "Ch 1",
            chapterScanlator = null,
            chapterUrl = "/u",
            mangaTitle = "Absent",
            source = source,
        ).shouldBeNull()
    }

    @Test
    fun findChapterDirsReturnsMatches() {
        chapterDir(mangaTitle = "Title", name = "Ch 1")
        val chapters = listOf(testChapter(name = "Ch 1"), testChapter(name = "Ch 2"))
        val (mangaDir, dirs) = harness.provider.findChapterDirs(
            chapters = chapters,
            manga = testManga("Title"),
            source = source,
        )
        mangaDir!!.name shouldBe "Title"
        dirs.map { it.name } shouldContainExactly listOf("Ch 1")
    }

    @Test
    fun findChapterDirsWithoutMangaDir() {
        val (mangaDir, dirs) = harness.provider.findChapterDirs(
            chapters = listOf(testChapter(name = "Ch 1")),
            manga = testManga("Absent"),
            source = source,
        )
        mangaDir.shouldBeNull()
        dirs.isEmpty() shouldBe true
    }

    @Test
    fun unmatchedKeepsTempAndStranger() {
        chapterDir(mangaTitle = "Title", name = "Ch 1")
        chapterDir(mangaTitle = "Title", name = "Ch 9${Downloader.TMP_DIR_SUFFIX}")
        chapterDir(mangaTitle = "Title", name = "stranger")
        val chapters = listOf(testChapter(name = "Ch 1"))
        val unmatched = harness.provider.findUnmatchedChapterDirs(
            chapters = chapters,
            manga = testManga("Title"),
            source = source,
        )
        unmatched.map { it.name } shouldContainExactly listOf("Ch 9${Downloader.TMP_DIR_SUFFIX}")
        harness.provider.findUnmatchedChapterDirs(
            chapters = chapters,
            manga = testManga("Absent"),
            source = source,
        ).isEmpty() shouldBe true
    }

    @Test
    fun unmatchedKeepsUnknown() {
        chapterDir(mangaTitle = "Title", name = "Ch 1")
        chapterDir(mangaTitle = "Title", name = "stranger")
        val unmatched = harness.provider.findUnmatchedChapterDirs(
            chapters = listOf(testChapter(name = "Ch 99")),
            manga = testManga("Title"),
            source = source,
        )
        unmatched.mapNotNull { it.name }.sorted() shouldContainExactly listOf("Ch 1", "stranger")
    }
}
