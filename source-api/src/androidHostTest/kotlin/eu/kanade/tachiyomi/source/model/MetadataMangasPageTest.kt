package eu.kanade.tachiyomi.source.model

import exh.metadata.metadata.RankedSearchMetadata
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class MetadataMangasPageTest {
    private val manga = SManga("/u", "Title")
    private val meta = RankedSearchMetadata()
    private val page = MetadataMangasPage(listOf(manga), true, listOf(meta))
    private val keyed = MetadataMangasPage(
        mangas = listOf(manga),
        hasNextPage = true,
        mangasMetadata = listOf(meta),
        nextKey = 7L,
    )

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun nextKeyDefaultsToNull() {
        page.nextKey shouldBe null
        keyed.nextKey shouldBe 7L
        page.mangasMetadata shouldBe listOf(meta)
    }

    @Test
    fun copyKeepsFields() {
        val copy = keyed.copy(nextKey = keyed.nextKey)
        copy shouldBe keyed
        copy.hashCode() shouldBe keyed.hashCode()
    }

    @Test
    fun copyReplacesFields() {
        val copy = keyed.copy(mangas = emptyList(), hasNextPage = false, mangasMetadata = emptyList(), nextKey = null)
        copy shouldBe MetadataMangasPage(
            mangas = emptyList(),
            hasNextPage = false,
            mangasMetadata = emptyList(),
            nextKey = null,
        )
        copy shouldNotBe keyed
        val partial = keyed.copy(mangas = emptyList(), hasNextPage = false, nextKey = keyed.nextKey)
        partial shouldBe MetadataMangasPage(
            mangas = emptyList(),
            hasNextPage = false,
            mangasMetadata = listOf(meta),
            nextKey = 7L,
        )
    }

    @Test
    fun identityAndNull() {
        val same: Any = page
        (page == same) shouldBe true
        val nothing: Any? = null
        (page == nothing) shouldBe false
    }

    @Test
    fun differentClassIsNotEqual() {
        val plain: Any = MangasPage(listOf(manga), true)
        (page == plain) shouldBe false
    }

    @Test
    fun baseMismatchIsNotEqual() {
        (page == MetadataMangasPage(emptyList(), true, listOf(meta))) shouldBe false
        (page == MetadataMangasPage(listOf(manga), false, listOf(meta))) shouldBe false
    }

    @Test
    fun mangasChangingBetweenReads() {
        val other = spyk(MetadataMangasPage(listOf(manga), true, listOf(meta)))
        every { other.mangas } returnsMany listOf(listOf(manga), emptyList())
        other.javaClass shouldBe page.javaClass
        (page == other) shouldBe false
    }

    @Test
    fun nextPageChangingBetweenReads() {
        val other = spyk(MetadataMangasPage(listOf(manga), true, listOf(meta)))
        every { other.hasNextPage } returnsMany listOf(true, false)
        other.javaClass shouldBe page.javaClass
        (page == other) shouldBe false
    }

    @Test
    fun metadataAndKeyMismatch() {
        (page == MetadataMangasPage(listOf(manga), true, emptyList())) shouldBe false
        (page == keyed) shouldBe false
        page shouldBe MetadataMangasPage(listOf(manga), true, listOf(meta))
        keyed.copy(mangasMetadata = keyed.mangasMetadata) shouldBe keyed
    }

    @Test
    fun hashCodeAndToString() {
        var expected = 31 * listOf(manga).hashCode() + true.hashCode()
        expected = 31 * expected + listOf(manga).hashCode()
        expected = 31 * expected + true.hashCode()
        expected = 31 * expected + listOf(meta).hashCode()
        page.hashCode() shouldBe 31 * expected
        keyed.hashCode() shouldBe 31 * expected + 7L.hashCode()
        val expectedText = "MetadataMangasPage(mangas=${listOf(manga)}, hasNextPage=true, " +
            "mangasMetadata=${listOf(meta)}, nextKey=7)"
        keyed.toString() shouldBe expectedText
    }
}
