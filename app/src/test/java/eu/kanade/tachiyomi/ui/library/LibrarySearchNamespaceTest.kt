package eu.kanade.tachiyomi.ui.library

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LibrarySearchNamespaceTest {
    private val harness = LibraryHarness()
    private val rig = LibrarySearchRig(harness)
    private val manga by lazy { manga(1, "Title") }
    private val tags by lazy { SearchExtras(tags = listOf(tag("big", "female"), tag("plain"), tag("bare", ""))) }

    @Before
    fun setUp() {
        startKoin { modules(harness.koinModules()) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun has(name: String, tag: String? = null) = rig.matches(rig.namespace(name, tag), manga, tags)

    private fun lacks(name: String, tag: String? = null) =
        rig.matches(rig.namespace(name, tag, excluded = true), manga, tags)

    @Test
    fun namespacedTagsMatch() {
        has("female", "big").shouldBeTrue()
        has("FEMALE", "small").shouldBeFalse()
        has("male", "big").shouldBeFalse()
        has("female").shouldBeTrue()
        has("male").shouldBeFalse()
        rig.matches(rig.namespace("female", "big"), manga).shouldBeFalse()
    }

    @Test
    fun namespaceExclusion() {
        rig.matches(rig.namespace("female", "big", excluded = true), manga).shouldBeTrue()
        lacks("", "").shouldBeTrue()
        lacks("", "big").shouldBeFalse()
        lacks("", "zzz").shouldBeTrue()
        lacks("female").shouldBeFalse()
        lacks("male").shouldBeTrue()
        lacks("female", "big").shouldBeFalse()
        lacks("female", "small").shouldBeTrue()
        lacks("male", "big").shouldBeTrue()
    }

    @Test
    fun blankPartsOfTheExclusion() {
        lacks("").shouldBeTrue()
        lacks("female", " ").shouldBeFalse()
        lacks("", "bare").shouldBeFalse()
    }

    @Test
    fun otherComponentsPass() {
        rig.matches(exh.search.QueryComponent(), manga).shouldBeTrue()
    }
}
