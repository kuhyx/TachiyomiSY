package tachiyomi.source.local

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import mihon.core.common.archive.archiveReader
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import java.io.File
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
internal class ComicInfoFilesTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val files = ComicInfoFiles(context, testXml)

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun titleOf(stream: InputStream): String? = files.parse(stream).title?.value

    @Test
    fun encodeAndParseRoundTrip() {
        val info = emptyComicInfo().copy(series = ComicInfo.Series("Manga"), writer = ComicInfo.Writer("W"))
        files.parse(files.encode(info).byteInputStream()) shouldBe info
    }

    @Test
    fun appliesMetadataToTheManga() {
        val manga = sampleManga("m")
        val info = emptyComicInfo().copy(
            series = ComicInfo.Series("Series"),
            summary = ComicInfo.Summary("Sum"),
            writer = ComicInfo.Writer("W"),
            penciller = ComicInfo.Penciller("P"),
            genre = ComicInfo.Genre("Action"),
            publishingStatus = ComicInfo.PublishingStatusTachiyomi("Ongoing"),
        )
        files.applyToManga(comicInfoXml(info).inputStream(), manga)
        manga.title shouldBe "Series"
        manga.description shouldBe "Sum"
        manga.author shouldBe "W"
        manga.artist shouldBe "P"
        manga.genre shouldBe "Action"
        manga.status shouldBe SManga.ONGOING
    }

    @Test
    fun appliesChapterFields() {
        val chapter = sampleChapter("m/ch")
        val info = emptyComicInfo().copy(
            title = ComicInfo.Title("Named"),
            number = ComicInfo.Number("3.5"),
            translator = ComicInfo.Translator("Group"),
        )
        files.applyToChapter(comicInfoXml(info).inputStream(), chapter)
        chapter.name shouldBe "Named"
        chapter.chapter_number shouldBe 3.5f
        chapter.scanlator shouldBe "Group"
    }

    @Test
    fun absentChapterFieldsAreKept() {
        val chapter = sampleChapter("m/ch", "Original")
        files.applyToChapter(comicInfoXml(emptyComicInfo()).inputStream(), chapter)
        chapter.name shouldBe "Original"
        chapter.chapter_number shouldBe -1f
        chapter.scanlator.shouldBeNull()
        files.applyToChapter(comicInfoXml(emptyComicInfo().copy(number = ComicInfo.Number("x"))).inputStream(), chapter)
        chapter.chapter_number shouldBe -1f
    }

    @Test
    fun readsComicInfoOfAFolder() {
        val chapter = folder.newFolder("ch1")
        File(chapter, COMIC_INFO_FILE).writeBytes(comicInfoXml(emptyComicInfo().copy(title = ComicInfo.Title("T"))))
        files.forChapter(chapter.uni()) { stream, encrypted -> titleOf(stream) to encrypted } shouldBe ("T" to false)
        files.forChapter(folder.newFolder("ch2").uni()) { _, _ -> "never" }.shouldBeNull()
    }

    @Test
    fun readsComicInfoOfAnArchive() {
        val xml = comicInfoXml(emptyComicInfo().copy(title = ComicInfo.Title("A")))
        stubArchiveReaders(fakeArchiveReader(mapOf(COMIC_INFO_FILE to xml), encrypted = true))
        val archive = folder.newFile("ch1.cbz").uni()
        files.forChapter(archive) { stream, encrypted -> titleOf(stream) to encrypted } shouldBe ("A" to true)
    }

    @Test
    fun archiveWithoutComicInfoIsNull() {
        stubArchiveReaders(fakeArchiveReader(emptyMap()))
        files.forChapter(folder.newFile("ch1.cbz").uni()) { _, _ -> "never" }.shouldBeNull()
    }

    @Test
    fun copiesTheFirstComicInfoFound() {
        val xml = comicInfoXml(emptyComicInfo().copy(title = ComicInfo.Title("C")))
        val first = folder.newFile("ch1.cbz").uni()
        val second = folder.newFile("ch2.cbz").uni()
        mockkStatic("mihon.core.common.archive.ArchiveReaderKt")
        every { first.archiveReader(any()) } returns fakeArchiveReader(emptyMap())
        every { second.archiveReader(any()) } returns fakeArchiveReader(mapOf(COMIC_INFO_FILE to xml))
        val target = folder.newFolder("manga")
        files.copyFromChapters(listOf(first, second), target.uni())?.name shouldBe COMIC_INFO_FILE
        File(target, COMIC_INFO_FILE).readBytes() shouldBe xml
        files.copyFromChapters(listOf(first), folder.newFolder("other").uni()).shouldBeNull()
    }

    @Test
    fun copyWritesAPlainComicInfo() {
        val target = folder.newFolder("manga")
        files.copy("<x/>".byteInputStream(), target.uni(), encrypt = false)?.name shouldBe COMIC_INFO_FILE
        File(target, COMIC_INFO_FILE).readText() shouldBe "<x/>"
    }

    @Test
    fun uncreatableFileGivesNull() {
        val target = folder.newFolder("manga")
        File(target, COMIC_INFO_FILE).mkdir()
        files.copy("<x/>".byteInputStream(), target.uni(), encrypt = false).shouldBeNull()
    }
}
