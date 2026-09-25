package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.util.lang.Hash
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadProviderNamesTest {

    private val harness = DownloadProviderHarness(root = null)
    private val provider = harness.provider

    private fun hashOf(url: String): String = "_" + Hash.md5(url).take(URL_HASH_CHARS)

    @Test
    fun sourceAndMangaSanitized() {
        provider.getSourceDirName(namedSource("My: Source")) shouldBe "My_ Source"
        provider.getMangaDirName("Man/ga?") shouldBe "Man_ga_"
    }

    @Test
    fun nonAsciiIsHexEncodedWhenAsked() {
        harness.libraryPreferences.disallowNonAsciiFilenames.set(true)
        // "è" is 0xC3 0xA8 in UTF-8.
        provider.getMangaDirName("Cafè") shouldBe "Cafc3a8"
        provider.getSourceDirName(namedSource("Sourcè")) shouldBe "Sourcc3a8"
    }

    @Test
    fun blankChapterNameBecomesChapter() {
        provider.sanitizeChapterName("   ") shouldBe "Chapter"
        provider.sanitizeChapterName("Ch 1") shouldBe "Ch 1"
    }

    @Test
    fun chapterDirNameWithoutHash() {
        harness.downloadPreferences.includeChapterUrlHash.set(false)
        provider.getChapterDirName(chapterName = "Ch 1", chapterScanlator = null, chapterUrl = "/u") shouldBe "Ch 1"
    }

    @Test
    fun chapterDirPrefixesScanlator() {
        harness.downloadPreferences.includeChapterUrlHash.set(false)
        provider.getChapterDirName(
            chapterName = "Ch 1",
            chapterScanlator = "Team",
            chapterUrl = "/u",
        ) shouldBe "Team_Ch 1"
        provider.getChapterDirName(chapterName = "Ch 1", chapterScanlator = " ", chapterUrl = "/u") shouldBe "Ch 1"
    }

    @Test
    fun chapterDirNameAppendsUrlHash() {
        harness.downloadPreferences.includeChapterUrlHash.set(true)
        provider.getChapterDirName(
            chapterName = "Ch 1",
            chapterScanlator = null,
            chapterUrl = "/u",
        ) shouldBe "Ch 1" + hashOf("/u")
    }

    @Test
    fun legacyNamesFlipBothSettings() {
        harness.downloadPreferences.includeChapterUrlHash.set(true)
        val names = provider.getLegacyChapterDirNames(
            chapterName = "Ch 1",
            chapterScanlator = "Team",
            chapterUrl = "/u",
        )
        names shouldContainExactly listOf("Team_Ch 1", "Team_Ch 1")
        provider.getLegacyChapterDirNames(
            chapterName = "Ch 1",
            chapterScanlator = null,
            chapterUrl = "/u",
        ) shouldContainExactly listOf("Ch 1", "Ch 1")
        harness.libraryPreferences.disallowNonAsciiFilenames.set(true)
        provider.getLegacyChapterDirNames(
            chapterName = "Ch 1",
            chapterScanlator = "  ",
            chapterUrl = "/u",
        ) shouldContainExactly listOf("Ch 1", "Ch 1")
    }

    @Test
    fun validNamesCoverNullScanlator() {
        harness.downloadPreferences.includeChapterUrlHash.set(false)
        val names = provider.getValidChapterDirNames(
            chapterName = "Ch 1",
            chapterScanlator = null,
            chapterUrl = "/u",
        )
        names.take(4) shouldContainExactly listOf("Ch 1", "Ch 1.cbz", "_Ch 1", "_Ch 1.cbz")
        names.size shouldBe 8
        provider.getValidChapterDirNames(
            chapterName = "Ch 1",
            chapterScanlator = "  ",
            chapterUrl = "/u",
        ).take(3) shouldContainExactly listOf("Ch 1", "Ch 1.cbz", "_Ch 1")
    }

    @Test
    fun validNamesCoverScanlator() {
        harness.downloadPreferences.includeChapterUrlHash.set(false)
        val names = provider.getValidChapterDirNames(
            chapterName = "Ch 1",
            chapterScanlator = "Team",
            chapterUrl = "/u",
        )
        names.take(3) shouldContainExactly listOf("Team_Ch 1", "Team_Ch 1.cbz", "Ch 1")
        names.size shouldBe 7
    }

    @Test
    fun dirNameChangeIsDetected() {
        harness.downloadPreferences.includeChapterUrlHash.set(true)
        val old = testChapter(name = "Ch 1", url = "/one")
        provider.isChapterDirNameChanged(oldChapter = old, newChapter = old.copy(url = "/two")) shouldBe true
        provider.isChapterDirNameChanged(oldChapter = old, newChapter = old.copy(name = "Ch 1")) shouldBe false
    }
}
