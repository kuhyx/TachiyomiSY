package exh.util

import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SourceTagsUtilColorTest {
    @Test
    fun genreColorsParseToArgb() {
        SourceTagsUtil.GenreColor.DOUJINSHI_COLOR.color shouldBe 0xFFF44336.toInt()
        SourceTagsUtil.GenreColor.MISC_COLOR.color shouldBe 0xFFF06292.toInt()
        SourceTagsUtil.GenreColor.entries.size shouldBe 10
        SourceTagsUtil.GenreColor.entries.map { it.color }.shouldBeUnique()
    }
}
