package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.fullChapter

internal class ShouldUpdateDbChapterTest {

    private val stored = fullChapter()
    private val shouldUpdate = ShouldUpdateDbChapter()

    @Test
    fun identicalRowsNeedNoUpdate() {
        shouldUpdate.await(stored, fullChapter()) shouldBe false
    }

    @Test
    fun readerStateIsIgnored() {
        val source = stored.copy(read = false, bookmark = false, lastPageRead = 0L, id = 99L)

        shouldUpdate.await(stored, source) shouldBe false
    }

    @Test
    fun eachSourceFieldTriggers() {
        shouldUpdate.await(stored, stored.copy(scanlator = null)) shouldBe true
        shouldUpdate.await(stored, stored.copy(name = "Renamed")) shouldBe true
        shouldUpdate.await(stored, stored.copy(dateUpload = 60L)) shouldBe true
        shouldUpdate.await(stored, stored.copy(chapterNumber = 1.5)) shouldBe true
        shouldUpdate.await(stored, stored.copy(sourceOrder = 50L)) shouldBe true
        shouldUpdate.await(stored, stored.copy(memo = JsonObject.EMPTY)) shouldBe true
    }
}
