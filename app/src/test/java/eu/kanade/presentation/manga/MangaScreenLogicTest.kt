package eu.kanade.presentation.manga

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MangaScreenLogicTest {
    private val events = mutableListOf<String>()

    private fun click(selected: Boolean, anySelected: Boolean) {
        onChapterItemClick(
            chapterItem = chapterItem(mangaChapter(4L), selected = selected),
            isAnyChapterSelected = anySelected,
            onToggleSelection = { events += "toggle $it" },
            onChapterClicked = { events += "open ${it.id}" },
        )
    }

    @Test
    fun clickTogglesOrOpens() {
        click(selected = true, anySelected = true)
        click(selected = false, anySelected = true)
        click(selected = false, anySelected = false)
        events shouldContainExactly listOf("toggle false", "toggle true", "open 4")
    }

    @Test
    fun downloadActionCounts() {
        DownloadAction.entries.map { it.nextChapters } shouldContainExactly listOf(1, 5, 10, 25, null, null)
        EditCoverAction.entries.map { it.name } shouldContainExactly listOf("EDIT", "DELETE")
        MangaScreenItem.entries.size shouldBe 10
        MangaScreenItem.valueOf("CHAPTER") shouldBe MangaScreenItem.CHAPTER
    }
}
