package tachiyomi.domain.manga.interactor

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository

internal class CustomMangaInfoInteractorsTest {

    private val info = CustomMangaInfo(id = 4L, title = "Custom")
    private val repository = mockk<CustomMangaRepository>()

    @Test
    fun getReturnsStoredInfo() {
        every { repository.get(4L) } returns info

        GetCustomMangaInfo(repository).get(4L) shouldBe info
    }

    @Test
    fun getReturnsNullWhenAbsent() {
        every { repository.get(5L) } returns null

        GetCustomMangaInfo(repository).get(5L) shouldBe null
    }

    @Test
    fun setDelegates() {
        every { repository.set(info) } returns Unit

        SetCustomMangaInfo(repository).set(info)

        verify(exactly = 1) { repository.set(info) }
    }
}
