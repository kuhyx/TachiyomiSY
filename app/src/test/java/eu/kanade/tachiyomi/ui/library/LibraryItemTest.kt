package eu.kanade.tachiyomi.ui.library

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.customInfoModule
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

internal class LibraryItemTest {
    private val source = mockk<Source> {
        every { name } returns "MangaSite"
        every { lang } returns "en"
    }
    private val sourcePreferences =
        SourcePreferences(MapPreferenceStore()).also { it.enabledLanguages.set(setOf("en")) }
    private val sourceManager = mockk<SourceManager> { every { getOrStub(any()) } returns source }
    private val full = manga(1, "One Piece").copy(
        ogAuthor = "Oda",
        ogArtist = "Eiichiro",
        ogDescription = "Pirates",
        ogGenre = listOf("Action", "Comedy"),
    )
    private val bare = manga(2, "Bare")

    @BeforeEach
    fun setUp() {
        startKoin {
            modules(
                customInfoModule(),
                module {
                    single { sourceManager }
                    single { sourcePreferences }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() = stopKoin()

    private fun item(manga: Manga) = libraryItem(manga, sourceManager = sourceManager)

    @Test
    fun idQueriesMatchExactly() {
        item(full).matches("id:1").shouldBeTrue()
        item(full).matches("id:2").shouldBeFalse()
        item(full).matches("ID:1").shouldBeFalse()
        item(full).matches("id:x").shouldBeFalse()
    }

    @Test
    fun textFieldsMatch() {
        item(full).matches("piece").shouldBeTrue()
        item(full).matches("oda").shouldBeTrue()
        item(full).matches("eiichiro").shouldBeTrue()
        item(full).matches("pirates").shouldBeTrue()
    }

    @Test
    fun sourceAndGenresMatch() {
        item(full).matches("mangasite").shouldBeTrue()
        item(full).matches("action, comedy").shouldBeTrue()
        item(full).matches("-horror").shouldBeTrue()
        item(full).matches("- action").shouldBeFalse()
        item(full).matches("horror").shouldBeFalse()
    }

    @Test
    fun missingFieldsDoNotMatch() {
        item(bare).matches("nothing").shouldBeFalse()
        item(bare).matches("-nothing").shouldBeTrue()
    }

    @Test
    fun itemIsAValue() {
        val item = item(full)
        item.id shouldBe 1L
        item.copy(downloadCount = 3).downloadCount shouldBe 3
        item.badges.copy(isLocal = true).isLocal shouldBe true
        LibraryItem(libraryManga = item.libraryManga, badges = item.badges).unreadCount shouldBe -1L
    }
}
