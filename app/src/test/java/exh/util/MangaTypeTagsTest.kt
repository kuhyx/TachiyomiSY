package exh.util

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Test

internal class MangaTypeTagsTest {
    @Test
    fun mangaTagLatinOrCyrillic() {
        isMangaTag("Shounen Manga").shouldBeTrue()
        isMangaTag("МАНГА").shouldBeTrue()
        isMangaTag("comic").shouldBeFalse()
    }

    @Test
    fun manhuaTagLatinOrCyrillic() {
        isManhuaTag("Manhua").shouldBeTrue()
        isManhuaTag("маньхуа").shouldBeTrue()
        isManhuaTag("manga").shouldBeFalse()
    }

    @Test
    fun manhwaTagLatinOrCyrillic() {
        isManhwaTag("MANHWA").shouldBeTrue()
        isManhwaTag("манхва").shouldBeTrue()
        isManhwaTag("manga").shouldBeFalse()
    }

    @Test
    fun comicTagLatinOrCyrillic() {
        isComicTag("Comics").shouldBeTrue()
        isComicTag("комикс").shouldBeTrue()
        isComicTag("manga").shouldBeFalse()
    }

    @Test
    fun webtoonTagEitherSpelling() {
        isWebtoonTag("Long Strip").shouldBeTrue()
        isWebtoonTag("webtoon").shouldBeTrue()
        isWebtoonTag("manga").shouldBeFalse()
    }
}
