package eu.kanade.presentation.manga.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun ehMetadata(vararg tags: RaisedTag) = EHentaiSearchMetadata().apply { this.tags.addAll(tags) }

@RunWith(RobolectricTestRunner::class)
internal class NamespaceTagsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun metadataTagsGroupByNamespace() {
        val chips = SearchMetadataChips(
            ehMetadata(
                RaisedTag("female", "glasses", EHentaiSearchMetadata.TAG_TYPE_NORMAL),
                RaisedTag("female", "light", EHentaiSearchMetadata.TAG_TYPE_LIGHT),
                RaisedTag(null, "plain", EHentaiSearchMetadata.TAG_TYPE_WEAK),
                RaisedTag("", "blank", EHentaiSearchMetadata.TAG_TYPE_NORMAL),
                RaisedTag("misc", "hidden", RaisedSearchMetadata.TAG_TYPE_VIRTUAL),
            ),
            EH_SOURCE_ID,
            null,
        )!!.tags
        chips.keys.toList() shouldContainExactly listOf("female", "")
        chips.getValue("female").map { it.border } shouldContainExactly listOf(2, 1)
        chips.getValue("").map { it.search } shouldContainExactly listOf("plain", "blank")
        chips.getValue("").map { it.border } shouldContainExactly listOf(null, 2)
        chips.getValue("female").first().search shouldNotBe "glasses"
    }

    @Test
    fun onlyEHentaiGradesTags() {
        val exh = SearchMetadataChips(ehMetadata(RaisedTag("a", "b", 0)), EXH_SOURCE_ID, null)!!
        exh.tags.getValue("a").single().border shouldBe 2
        val other = SearchMetadataChips(ehMetadata(RaisedTag("a", "b", 0)), 1L, null)!!
        other.tags.getValue("a").single().let {
            it.border shouldBe null
            it.search shouldBe "b"
        }
    }

    @Test
    fun namespacedGenresParse() {
        val chips = SearchMetadataChips(null, 1L, listOf("female: glasses", "male:beard"))!!.tags
        chips.getValue("female").single() shouldBe DisplayTag("female", "glasses", "female: glasses", null)
        chips.keys.toList() shouldContainExactly listOf("female", "male")
    }

    @Test
    fun plainGenresHaveNoChips() {
        SearchMetadataChips(null, 1L, listOf("female:glasses", "action")) shouldBe null
        SearchMetadataChips(null, 1L, null) shouldBe null
    }

    @Test
    fun namespaceRowsClick() {
        val clicked = mutableListOf<String>()
        val chips = SearchMetadataChips(null, 1L, listOf("female:glasses", ":bare"))!!
        compose.setContent {
            MaterialTheme { NamespaceTags(tags = chips, onClick = { clicked += it }) }
        }
        compose.onNodeWithText("female").performClick()
        compose.onNodeWithText("glasses").performClick()
        compose.onNodeWithText("bare").performClick()
        clicked shouldContainExactly listOf("female:glasses", ":bare")
    }

    @Test
    fun previewRendersGradedTags() {
        compose.setContent { NamespaceTagsPreview() }
        compose.onAllNodesWithText("Test").fetchSemanticsNodes().size shouldBe 2
    }
}
