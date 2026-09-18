package tachiyomi.data.manga

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.data.manga.CustomMangaRepositoryImpl.MangaJson
import tachiyomi.domain.manga.model.CustomMangaInfo
import java.io.File

internal class CustomMangaRepositoryImplTest {
    @TempDir
    lateinit var dir: File

    private val full = CustomMangaInfo(
        id = 1L, title = "Edited", author = "au", artist = "ar", thumbnailUrl = "thumb", description = "desc",
        genre = listOf("g1", "g2"), status = 2L,
    )

    private fun repository(): CustomMangaRepositoryImpl {
        val context = mockk<Context> { every { getExternalFilesDir(null) } returns dir }
        return CustomMangaRepositoryImpl(context)
    }

    private fun editsFile(): File = File(dir, "edits.json")

    @Test
    fun noFileGivesNoEdits() {
        repository().get(1L) shouldBe null
        editsFile().exists() shouldBe false
    }

    @Test
    fun loadsEntriesFromFile() {
        editsFile().writeText(
            """{"mangas":[{"id":1,"title":"Edited","author":"au","genre":["g1"],"status":2},""" +
                """{"title":"no id"},{"id":3,"title":" ","status":0}]}""",
        )

        val repository = repository()

        val expected = CustomMangaInfo(id = 1L, title = "Edited", author = "au", genre = listOf("g1"), status = 2L)
        repository.get(1L) shouldBe expected
        repository.get(2L) shouldBe null
        repository.get(3L) shouldBe CustomMangaInfo(id = 3L, title = null)
    }

    @Test
    fun malformedFileIsEmpty() {
        editsFile().writeText("not json")

        repository().get(1L) shouldBe null
    }

    @Test
    fun fileWithoutMangasIsEmpty() {
        editsFile().writeText("{}")

        repository().get(1L) shouldBe null
    }

    @Test
    fun setStoresAndPersists() {
        val repository = repository()

        repository.set(full)

        repository.get(1L) shouldBe full
        repository().get(1L) shouldBe full
    }

    @Test
    fun setEmptyRemovesEntry() {
        val repository = repository()
        repository.set(full)

        repository.set(CustomMangaInfo(id = 1L, title = null))

        repository.get(1L) shouldBe null
        editsFile().readText().contains("\"Edited\"") shouldBe true
    }

    @Test
    fun setEmptyWritesNoFile() {
        repository().set(CustomMangaInfo(id = 1L, title = null))

        editsFile().exists() shouldBe false
    }

    @Test
    fun setOverwritesExistingFile() {
        val repository = repository()
        repository.set(full)

        repository.set(full.copy(id = 2L, title = "Other"))

        val reloaded = repository()
        reloaded.get(1L) shouldBe full
        reloaded.get(2L)?.title shouldBe "Other"
    }

    @Test
    fun toMangaDropsBlankAndZero() {
        val repository = repository()

        val blank = repository.run { MangaJson(id = 4L, title = " ", status = 0L).toManga() }
        val kept = repository.run { MangaJson(id = 5L, title = "T", status = 3L).toManga() }
        val absent = repository.run { MangaJson(id = 6L).toManga() }

        blank shouldBe CustomMangaInfo(id = 4L, title = null)
        kept shouldBe CustomMangaInfo(id = 5L, title = "T", status = 3L)
        absent shouldBe CustomMangaInfo(id = 6L, title = null)
    }

    @Test
    fun toJsonRoundTrips() {
        val repository = repository()

        val json = repository.run { full.toJson() }

        json shouldBe MangaJson(
            id = 1L, title = "Edited", author = "au", artist = "ar", thumbnailUrl = "thumb", description = "desc",
            genre = listOf("g1", "g2"), status = 2L,
        )
        repository.run { json.toManga() } shouldBe full
    }
}
