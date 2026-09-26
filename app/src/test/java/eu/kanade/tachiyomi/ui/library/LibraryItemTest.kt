package eu.kanade.tachiyomi.ui.library

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.source.service.SourceManager

internal class LibraryItemTest {
    private val source = mockk<Source> {
        every { name } returns "Dex"
        every { lang } returns "en"
    }
    private val sourceManager = mockk<SourceManager> { every { getOrStub(any()) } returns source }

    @BeforeEach
    fun setUp() {
        val preferences = SourcePreferences(FlowPreferenceStore())
        preferences.enabledLanguages.set(setOf("en"))
        startKoin {
            modules(
                module {
                    single { preferences }
                    single { sourceManager }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun item(
        author: String? = null,
        artist: String? = null,
        description: String? = null,
        genre: List<String>? = null,
    ): LibraryItem {
        val manga = libManga(1L, title = "Needle").copy(
            ogAuthor = author,
            ogArtist = artist,
            ogDescription = description,
            ogGenre = genre,
        )
        return LibraryItem(libraryManga = libEntry(manga), sourceManager = sourceManager, badges = badges())
    }

    private fun badges() = LibraryItem.Badges(downloadCount = 0, unreadCount = 0L, isLocal = false, sourceLanguage = "")

    @Test
    fun idConstraintNamesTheEntry() {
        item().matches("id:1") shouldBe true
        item().matches("ID:2") shouldBe false
        item().matches("id:x") shouldBe false
        item().matches("id:5") shouldBe false
    }

    @Test
    fun blankFieldsNeverMatch() {
        item().matches("needle") shouldBe true
        item().matches("zzz") shouldBe false
    }

    @Test
    fun peopleAndDescriptionMatch() {
        val full = item(author = "Aa", artist = "Bb", description = "Cc", genre = listOf("Action"))
        full.matches("aa") shouldBe true
        full.matches("bb") shouldBe true
        full.matches("cc") shouldBe true
        full.matches("zz") shouldBe false
    }

    @Test
    fun genresAndSourceByParts() {
        val full = item(author = "Aa", artist = "Bb", description = "Cc", genre = listOf("Action"))
        full.matches("action, -romance") shouldBe true
        full.matches("action, -action") shouldBe false
        full.matches("dex") shouldBe true
    }

    @Test
    fun defaultsComeFromInjekt() {
        val item = LibraryItem(libraryManga = libEntry(libManga(3L)), badges = badges())
        item.sourceManager shouldBe sourceManager
        item.id shouldBe 3L
        item.downloadCount shouldBe -1
        item.unreadCount shouldBe -1L
        item.isLocal shouldBe false
        item.sourceLanguage shouldBe ""
        item.copy(isLocal = true) shouldNotBe item
        item.copy().hashCode() shouldBe item.hashCode()
        badges().copy(isLocal = true).toString().isNotEmpty() shouldBe true
    }
}
