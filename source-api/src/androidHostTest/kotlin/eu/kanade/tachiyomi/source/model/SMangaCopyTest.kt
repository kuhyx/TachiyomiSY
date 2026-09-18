package eu.kanade.tachiyomi.source.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import org.junit.jupiter.api.Test

/** The `SManga.copy(...)` extension, each default argument supplied and left out. */
internal class SMangaCopyTest {
    private val source = populatedManga()

    @Test
    fun oneArgumentKeepsTheRest() {
        // A bare `copy()` resolves to the member; the extension needs at least one argument.
        val copy = source.copy(url = "/new")
        copy shouldNotBeSameInstanceAs source
        copy.url shouldBe "/new"
        copy.title shouldBe "Title"
        copy.artist shouldBe "Artist"
        copy.author shouldBe "Author"
        copy.description shouldBe "Description"
        copy.genre shouldBe "Action, Drama"
        copy.status shouldBe SManga.COMPLETED
        copy.thumbnail_url shouldBe "https://example.invalid/cover.png"
        copy.initialized shouldBe true
    }

    @Test
    fun allArgumentsReplaceEverything() {
        val copy = source.copy(
            url = "/other",
            title = "Other",
            artist = "A2",
            author = "B2",
            description = "D2",
            genre = "G2",
            status = SManga.ONGOING,
            thumbnailUrl = "https://example.invalid/other.png",
            initialized = false,
        )
        copy.url shouldBe "/other"
        copy.title shouldBe "Other"
        copy.artist shouldBe "A2"
        copy.author shouldBe "B2"
        copy.description shouldBe "D2"
        copy.genre shouldBe "G2"
        copy.status shouldBe SManga.ONGOING
        copy.thumbnail_url shouldBe "https://example.invalid/other.png"
        copy.initialized shouldBe false
    }

    @Test
    fun titleAlone() {
        val copy = source.copy(title = "Renamed")
        copy.title shouldBe "Renamed"
        copy.url shouldBe "/manga/1"
    }

    @Test
    fun creditsAlone() {
        source.copy(artist = null).artist shouldBe null
        source.copy(author = null).author shouldBe null
        source.copy(artist = "X").author shouldBe "Author"
    }

    @Test
    fun textFieldsAlone() {
        source.copy(description = null).description shouldBe null
        source.copy(genre = null).genre shouldBe null
        source.copy(description = "X").genre shouldBe "Action, Drama"
    }

    @Test
    fun statusAlone() {
        val copy = source.copy(status = SManga.ON_HIATUS)
        copy.status shouldBe SManga.ON_HIATUS
        copy.initialized shouldBe true
    }

    @Test
    fun thumbnailAlone() {
        val copy = source.copy(thumbnailUrl = null)
        copy.thumbnail_url shouldBe null
        copy.status shouldBe SManga.COMPLETED
    }

    @Test
    fun initializedAlone() {
        val copy = source.copy(initialized = false)
        copy.initialized shouldBe false
        copy.thumbnail_url shouldBe "https://example.invalid/cover.png"
    }

    @Test
    fun copiesOriginalsNotEdits() {
        val edited = VanishingGenreManga()
        edited.url = "/v"
        edited.title = "Vanishing"
        val copy = edited.copy(url = "/w")
        copy.url shouldBe "/w"
        copy.title shouldBe "Vanishing"
        copy.genre shouldBe "Action"
        edited.genreReadCount() shouldBe 1
    }
}
