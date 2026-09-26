package eu.kanade.tachiyomi.ui.library

import exh.metadata.sql.models.SearchTag
import exh.search.Namespace
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LibrarySearchNamespaceTest {
    private val parts = LibrarySearchParts()

    @Before
    fun setUp() = parts.start()

    @After
    fun tearDown() = parts.stop()

    private fun passes(component: Namespace, vararg tags: SearchTag): Boolean = parts.search.filterManga(
        queries = listOf(component),
        libraryManga = parts.full,
        tracks = null,
        source = null,
        searchTags = tags.toList(),
        loggedInTrackServices = emptyMap(),
    )

    @Test
    fun namespaceNeedsTags() {
        parts.barePasses(namespace("artist")) shouldBe false
        parts.barePasses(namespace("artist", excluded = true)) shouldBe true
    }

    @Test
    fun namespaceWithTagMatchesBoth() {
        passes(namespace("artist", "bob"), searchTag("Artist", "Bobby")) shouldBe true
        passes(namespace("artist", "bob"), searchTag("artist", "alice")) shouldBe false
        passes(namespace("artist", "bob"), searchTag("group", "bob")) shouldBe false
    }

    @Test
    fun bareNamespaceMatchesAnyTag() {
        passes(namespace("artist"), searchTag("artist", "x")) shouldBe true
        passes(namespace("artist"), searchTag("group", "x")) shouldBe false
    }

    @Test
    fun blankExclusionKeepsEverything() {
        passes(namespace("", excluded = true), searchTag("artist", "x")) shouldBe true
        passes(namespace("", "", excluded = true), searchTag("artist", "x")) shouldBe true
        // An empty tag reads as no tag at all.
        passes(namespace("artist", "", excluded = true), searchTag("group", "x")) shouldBe true
    }

    @Test
    fun tagOnlyExclusion() {
        passes(namespace("", "bob", excluded = true), searchTag("artist", "bob")) shouldBe false
        passes(namespace("", "bob", excluded = true), searchTag("artist", "alice")) shouldBe true
    }

    @Test
    fun namespaceOnlyExclusion() {
        passes(namespace("artist", excluded = true), searchTag(null, "x")) shouldBe true
        passes(namespace("artist", excluded = true), searchTag("Artist", "x")) shouldBe false
        passes(namespace("artist", excluded = true), searchTag("group", "x")) shouldBe true
    }

    @Test
    fun namespacedTagExclusion() {
        val component = namespace("artist", "bob", excluded = true)
        passes(component, searchTag(null, "bob")) shouldBe true
        passes(component, searchTag("", "bob")) shouldBe true
        passes(component, searchTag("artist", "bob")) shouldBe false
        passes(component, searchTag("artist", "alice")) shouldBe true
        passes(component, searchTag("group", "bob")) shouldBe true
    }
}
