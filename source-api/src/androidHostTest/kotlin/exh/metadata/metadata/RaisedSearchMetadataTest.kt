package exh.metadata.metadata

import exh.metadata.metadata.RaisedSearchMetadata.Companion.toGenreList
import exh.metadata.metadata.RaisedSearchMetadata.Companion.toGenreString
import exh.metadata.metadata.base.RaisedTag
import exh.metadata.metadata.base.RaisedTitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class RaisedSearchMetadataTest {
    private val artist = RaisedTag(namespace = "artist", name = "ann", type = 0)
    private val bare = RaisedTag(namespace = null, name = "loose", type = 0)
    private val virtual = RaisedTag(namespace = "meta", name = "hidden", type = RaisedSearchMetadata.TAG_TYPE_VIRTUAL)
    private val language = RaisedTag(namespace = "language", name = "english", type = 1)

    private fun tagged(): RankedSearchMetadata = RankedSearchMetadata().apply {
        tags += listOf(artist, bare, virtual, language)
    }

    @Test
    fun getTitleOfTypeFindsStoredTitle() {
        val metadata = RankedSearchMetadata()
        metadata.titles += RaisedTitle(title = "main", type = 0)
        metadata.titles += RaisedTitle(title = "alt", type = 1)
        metadata.getTitleOfType(1) shouldBe "alt"
        metadata.getTitleOfType(2) shouldBe null
    }

    @Test
    fun replaceTitleSwapsOrRemoves() {
        val metadata = RankedSearchMetadata()
        metadata.titles += RaisedTitle(title = "old", type = 0)
        metadata.titles += RaisedTitle(title = "keep", type = 1)
        metadata.replaceTitleOfType(0, "new")
        metadata.titles shouldContainExactly listOf(
            RaisedTitle(title = "keep", type = 1),
            RaisedTitle(title = "new", type = 0),
        )
        metadata.replaceTitleOfType(0, null)
        metadata.titles shouldContainExactly listOf(RaisedTitle(title = "keep", type = 1))
    }

    @Test
    fun tagsToGenreStringSkipsVirtual() {
        tagged().tagsToGenreString() shouldBe "artist: ann, loose, language: english"
        RankedSearchMetadata().tagsToGenreString() shouldBe ""
    }

    @Test
    fun tagsToGenreListSkipsVirtual() {
        tagged().tagsToGenreList() shouldContainExactly listOf("artist: ann", "loose", "language: english")
        RankedSearchMetadata().tagsToGenreList() shouldBe emptyList()
    }

    @Test
    fun companionGenreHelpers() {
        val tags = mutableListOf(artist, virtual, bare)
        tags.toGenreString() shouldBe "artist: ann, loose"
        tags.toGenreList() shouldContainExactly listOf("artist: ann", "loose")
        mutableListOf(virtual).toGenreString() shouldBe ""
        mutableListOf(virtual).toGenreList() shouldBe emptyList()
    }

    @Test
    fun descriptionGroupsByNamespace() {
        val metadata = tagged()
        metadata.tags += RaisedTag(namespace = "artist", name = "bob", type = 0)
        val expected = listOf("Tags:", "▪ artist: <ann> <bob>", "<loose>", "▪ language: <english>", "")
        metadata.tagsToDescription().toString() shouldBe expected.joinToString("\n")
    }

    @Test
    fun tagsToDescriptionWithoutTags() {
        RankedSearchMetadata().tagsToDescription().toString() shouldBe "Tags:\n"
    }

    @Test
    fun titleDelegateReadsAndWrites() {
        val metadata = EHentaiSearchMetadata()
        metadata.title shouldBe null
        metadata.title = "romaji"
        metadata.altTitle = "日本語"
        metadata.titles shouldContainExactly listOf(
            RaisedTitle(title = "romaji", type = 0),
            RaisedTitle(title = "日本語", type = 1),
        )
        metadata.title shouldBe "romaji"
        metadata.altTitle shouldBe "日本語"
        metadata.title = null
        metadata.titles shouldContainExactly listOf(RaisedTitle(title = "日本語", type = 1))
    }

    @Test
    fun titleDelegateCanBeUsedDirectly() {
        val metadata = RankedSearchMetadata()
        val delegate = RaisedSearchMetadata.titleDelegate(7)
        delegate.getValue(metadata, EHentaiSearchMetadata::title) shouldBe null
        delegate.setValue(metadata, EHentaiSearchMetadata::title, "seven")
        delegate.getValue(metadata, EHentaiSearchMetadata::title) shouldBe "seven"
        metadata.titles shouldContainExactly listOf(RaisedTitle(title = "seven", type = 7))
    }

    @Test
    fun flattenJsonIgnoresUnknownKeys() {
        val decoded = RaisedSearchMetadata.raiseFlattenJson
            .decodeFromString(RankedSearchMetadata.serializer(), """{"rank":1,"legacy":true}""")
        decoded.rank shouldBe 1
    }
}
