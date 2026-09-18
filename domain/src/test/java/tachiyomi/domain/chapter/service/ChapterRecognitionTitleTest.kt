package tachiyomi.domain.chapter.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
internal class ChapterRecognitionTitleTest {

    @Test
    fun `Version before number`() {
        val mangaTitle = "Onepunch-Man"

        assertChapter(mangaTitle, "Onepunch-Man Punch Ver002 086 : Creeping Darkness [3]", 86.0)
    }

    @Test
    fun `Version attached to number`() {
        val mangaTitle = "Ansatsu Kyoushitsu"

        assertChapter(mangaTitle, "Ansatsu Kyoushitsu 011v002: Assembly Time", 11.0)
    }

    /**
     * Case where the chapter title contains the chapter
     * But wait it's not actual the chapter number.
     */
    @Test
    fun `Number after title with ch`() {
        val mangaTitle = "Tokyo ESP"

        assertChapter(mangaTitle, "Tokyo ESP 027: Part 002: Chapter 001", 027.0)
    }

    /**
     * Case where the chapter title contains the unwanted tag
     * But follow by chapter number.
     */
    @Test
    fun `Number after unwanted tag`() {
        val mangaTitle = "One-punch Man"

        assertChapter(mangaTitle, "Mag Version 195.5", 195.5)
    }

    @Test
    fun `Unparseable chapter`() {
        val mangaTitle = "random"

        assertChapter(mangaTitle, "Foo", -1.0)
    }

    @Test
    fun `Chapter with time in title`() {
        val mangaTitle = "random"

        assertChapter(mangaTitle, "Fairy Tail 404: 00:00", 404.0)
    }

    @Test
    fun `Alpha without dot`() {
        val mangaTitle = "random"

        assertChapter(mangaTitle, "Asu No Yoichi 19a", 19.1)
    }

    @Test
    fun `Title with extra and vol`() {
        val mangaTitle = "Fairy Tail"

        assertChapter(mangaTitle, "Fairy Tail 404.extravol002", 404.99)
        assertChapter(mangaTitle, "Fairy Tail 404 extravol002", 404.99)
    }

    @Test
    fun `Title with omake and vol`() {
        val mangaTitle = "Fairy Tail"

        assertChapter(mangaTitle, "Fairy Tail 404.omakevol002", 404.98)
        assertChapter(mangaTitle, "Fairy Tail 404 omakevol002", 404.98)
    }

    @Test
    fun `Title with special and vol`() {
        val mangaTitle = "Fairy Tail"

        assertChapter(mangaTitle, "Fairy Tail 404.specialvol002", 404.97)
        assertChapter(mangaTitle, "Fairy Tail 404 specialvol002", 404.97)
    }

    @Test
    fun `Title with commas`() {
        val mangaTitle = "One Piece"

        assertChapter(mangaTitle, "One Piece 300,a", 300.1)
        assertChapter(mangaTitle, "One Piece Ch,123,extra", 123.99)
        assertChapter(mangaTitle, "One Piece the sunny, goes swimming 024,005", 24.005)
    }

    @Test
    fun `Title with hyphens`() {
        val mangaTitle = "Solo Leveling"

        assertChapter(mangaTitle, "ch 122-a", 122.1)
        assertChapter(mangaTitle, "Solo Leveling Ch.123-extra", 123.99)
        assertChapter(mangaTitle, "Solo Leveling, 024-005", 24.005)
        assertChapter(mangaTitle, "Ch.191-200 Read Online", 191.200)
    }

    @Test
    fun `Chapters containing season`() {
        assertChapter("D.I.C.E", "D.I.C.E[Season 001] Ep. 007", 7.0)
    }

    @Test
    fun `Format sx - chapter xx`() {
        assertChapter("The Gamer", "S3 - Chapter 20", 20.0)
    }

    @Test
    fun `Chapters ending with s`() {
        assertChapter("One Outs", "One Outs 001", 1.0)
    }

    @Test
    fun `Chapters containing ordinals`() {
        val mangaTitle = "The Sister of the Woods with a Thousand Young"

        assertChapter(mangaTitle, "The 1st Night", 1.0)
        assertChapter(mangaTitle, "The 2nd Night", 2.0)
        assertChapter(mangaTitle, "The 3rd Night", 3.0)
        assertChapter(mangaTitle, "The 4th Night", 4.0)
    }
}
