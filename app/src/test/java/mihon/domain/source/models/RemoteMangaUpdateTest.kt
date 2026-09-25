package mihon.domain.source.models

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

internal class RemoteMangaUpdateTest {

    @Test
    fun isAValue() {
        val manga = Manga.create().copy(id = 1)
        val chapters = listOf(Chapter.create().copy(id = 2))
        val update = RemoteMangaUpdate(manga = manga, newChapters = chapters)
        update shouldBe RemoteMangaUpdate(manga, chapters)
        update.hashCode() shouldBe RemoteMangaUpdate(manga, chapters).hashCode()
        update.copy(newChapters = emptyList()).newChapters shouldBe emptyList()
        update.component1() shouldBe manga
        update.component2() shouldBe chapters
        update.toString().startsWith("RemoteMangaUpdate(manga=") shouldBe true
    }
}
