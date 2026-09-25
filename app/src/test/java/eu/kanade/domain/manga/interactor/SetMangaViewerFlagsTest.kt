package eu.kanade.domain.manga.interactor

import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

internal class SetMangaViewerFlagsTest {

    private val repository = mockk<MangaRepository>()
    private val interactor = SetMangaViewerFlags(repository)

    @Test
    fun replacesOnlyTheMaskedBits() = runTest {
        val existing = ReadingMode.WEBTOON.flagValue.toLong() or ReaderOrientation.LANDSCAPE.flagValue.toLong()
        coEvery { repository.getMangaById(4) } returns Manga.create().copy(id = 4, viewerFlags = existing)
        val updates = mutableListOf<MangaUpdate>()
        coEvery { repository.update(capture(updates)) } returns true
        interactor.awaitSetReadingMode(4, ReadingMode.LEFT_TO_RIGHT.flagValue.toLong())
        interactor.awaitSetOrientation(4, ReaderOrientation.PORTRAIT.flagValue.toLong())
        updates[0].viewerFlags shouldBe
            (ReadingMode.LEFT_TO_RIGHT.flagValue.toLong() or ReaderOrientation.LANDSCAPE.flagValue.toLong())
        updates[1].viewerFlags shouldBe
            (ReadingMode.WEBTOON.flagValue.toLong() or ReaderOrientation.PORTRAIT.flagValue.toLong())
    }
}
