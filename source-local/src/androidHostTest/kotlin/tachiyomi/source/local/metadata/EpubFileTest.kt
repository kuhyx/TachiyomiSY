package tachiyomi.source.local.metadata

import eu.kanade.tachiyomi.util.storage.EpubFile
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.source.local.EpubFixture
import tachiyomi.source.local.fakeArchiveReader
import tachiyomi.source.local.sampleChapter
import tachiyomi.source.local.sampleManga

@RunWith(RobolectricTestRunner::class)
internal class EpubFileTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun epub(metadata: String): EpubFile = EpubFile(fakeArchiveReader(EpubFixture.entries(metadata)))

    @Test
    fun fillsEveryFieldFromTheMetadata() {
        val manga = sampleManga("m")
        val chapter = sampleChapter("m/c", "old")
        epub(EpubFixture.FULL_METADATA).use { it.fillMetadata(manga, chapter) }
        manga.author shouldBe "Author A"
        manga.description shouldBe "Desc D"
        chapter.name shouldBe "Chapter One"
        chapter.scanlator shouldBe "Pub P"
        chapter.date_upload shouldBe EpubFixture.FULL_METADATA_DATE
    }

    @Test
    fun creatorStandsInForPublisher() {
        val chapter = sampleChapter("m/c")
        epub("<dc:creator>Solo</dc:creator>").use { it.fillMetadata(sampleManga("m"), chapter) }
        chapter.scanlator shouldBe "Solo"
    }

    @Test
    fun bareMetadataChangesNothing() {
        val manga = sampleManga("m")
        val chapter = sampleChapter("m/c", "old").apply { date_upload = 7L }
        epub("").use { it.fillMetadata(manga, chapter) }
        manga.author.shouldBeNull()
        manga.description.shouldBeNull()
        chapter.name shouldBe "old"
        chapter.scanlator.shouldBeNull()
        chapter.date_upload shouldBe 7L
    }

    @Test
    fun modifiedMetaStandsInForDate() {
        val chapter = sampleChapter("m/c")
        val metadata = """<meta property="dcterms:modified">2021-05-06T07:08:09+0000</meta>"""
        epub(metadata).use { it.fillMetadata(sampleManga("m"), chapter) }
        chapter.date_upload shouldBe 1_620_284_889_000L
    }

    @Test
    fun unparsableDateIsIgnored() {
        val chapter = sampleChapter("m/c").apply { date_upload = 9L }
        epub("<dc:date>yesterday</dc:date>").use { it.fillMetadata(sampleManga("m"), chapter) }
        chapter.date_upload shouldBe 9L
    }
}
