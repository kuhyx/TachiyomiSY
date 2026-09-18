package tachiyomi.domain.chapter.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
internal class ChapterRecognitionTest {

    @Test
    fun `Basic Ch prefix`() {
        val mangaTitle = "Mokushiroku Alice"

        assertChapter(mangaTitle, "Mokushiroku Alice Vol.1 Ch.4: Misrepresentation", 4.0)
    }

    @Test
    fun `Ch prefix, space after dot`() {
        val mangaTitle = "Mokushiroku Alice"

        assertChapter(mangaTitle, "Mokushiroku Alice Vol. 1 Ch. 4: Misrepresentation", 4.0)
    }

    @Test
    fun `Basic Ch prefix with decimal`() {
        val mangaTitle = "Mokushiroku Alice"

        assertChapter(mangaTitle, "Mokushiroku Alice Vol.1 Ch.4.1: Misrepresentation", 4.1)
        assertChapter(mangaTitle, "Mokushiroku Alice Vol.1 Ch.4.4: Misrepresentation", 4.4)
    }

    @Test
    fun `Ch prefix with alpha postfix`() {
        val mangaTitle = "Mokushiroku Alice"

        assertChapter(mangaTitle, "Mokushiroku Alice Vol.1 Ch.4.a: Misrepresentation", 4.1)
        assertChapter(mangaTitle, "Mokushiroku Alice Vol.1 Ch.4.b: Misrepresentation", 4.2)
        assertChapter(mangaTitle, "Mokushiroku Alice Vol.1 Ch.4.extra: Misrepresentation", 4.99)
    }

    @Test
    fun `Name containing one number`() {
        val mangaTitle = "Bleach"

        assertChapter(mangaTitle, "Bleach 567 Down With Snowwhite", 567.0)
    }

    @Test
    fun `One number and decimal`() {
        val mangaTitle = "Bleach"

        assertChapter(mangaTitle, "Bleach 567.1 Down With Snowwhite", 567.1)
        assertChapter(mangaTitle, "Bleach 567.4 Down With Snowwhite", 567.4)
    }

    @Test
    fun `One number and alpha`() {
        val mangaTitle = "Bleach"

        assertChapter(mangaTitle, "Bleach 567.a Down With Snowwhite", 567.1)
        assertChapter(mangaTitle, "Bleach 567.b Down With Snowwhite", 567.2)
        assertChapter(mangaTitle, "Bleach 567.extra Down With Snowwhite", 567.99)
    }

    @Test
    fun `Manga title and number`() {
        val mangaTitle = "Solanin"

        assertChapter(mangaTitle, "Solanin 028 Vol. 2", 28.0)
    }

    @Test
    fun `Manga title, decimal number`() {
        val mangaTitle = "Solanin"

        assertChapter(mangaTitle, "Solanin 028.1 Vol. 2", 28.1)
        assertChapter(mangaTitle, "Solanin 028.4 Vol. 2", 28.4)
    }

    @Test
    fun `Manga title and alpha number`() {
        val mangaTitle = "Solanin"

        assertChapter(mangaTitle, "Solanin 028.a Vol. 2", 28.1)
        assertChapter(mangaTitle, "Solanin 028.b Vol. 2", 28.2)
        assertChapter(mangaTitle, "Solanin 028.extra Vol. 2", 28.99)
    }

    @Test
    fun `Extreme case`() {
        val mangaTitle = "Onepunch-Man"

        assertChapter(mangaTitle, "Onepunch-Man Punch Ver002 028", 28.0)
    }

    @Test
    fun `Extreme case with decimal`() {
        val mangaTitle = "Onepunch-Man"

        assertChapter(mangaTitle, "Onepunch-Man Punch Ver002 028.1", 28.1)
        assertChapter(mangaTitle, "Onepunch-Man Punch Ver002 028.4", 28.4)
    }

    @Test
    fun `Extreme case with alpha`() {
        val mangaTitle = "Onepunch-Man"

        assertChapter(mangaTitle, "Onepunch-Man Punch Ver002 028.a", 28.1)
        assertChapter(mangaTitle, "Onepunch-Man Punch Ver002 028.b", 28.2)
        assertChapter(mangaTitle, "Onepunch-Man Punch Ver002 028.extra", 28.99)
    }

    @Test
    fun `Chapter containing dot v2`() {
        val mangaTitle = "random"

        assertChapter(mangaTitle, "Vol.1 Ch.5v.2: Alones", 5.0)
    }

    @Test
    fun `Number in manga title`() {
        val mangaTitle = "Ayame 14"

        assertChapter(mangaTitle, "Ayame 14 1 - The summer of 14", 1.0)
    }

    @Test
    fun `Space between ch x`() {
        val mangaTitle = "Mokushiroku Alice"

        assertChapter(mangaTitle, "Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation", 4.0)
    }

    @Test
    fun `Title with ch substring`() {
        val mangaTitle = "Ayame 14"

        assertChapter(mangaTitle, "Vol.1 Ch.1: March 25 (First Day Cohabiting)", 1.0)
    }

    @Test
    fun `Multiple zeros`() {
        val mangaTitle = "random"

        assertChapter(mangaTitle, "Vol.001 Ch.003: Kaguya Doesn't Know Much", 3.0)
    }
}

internal fun assertChapter(mangaTitle: String, name: String, expected: Double) {
    ChapterRecognition.parseChapterNumber(mangaTitle, name) shouldBe expected
}
