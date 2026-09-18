package eu.kanade.tachiyomi.source.model

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import xyz.nulldev.ts.api.http.serializer.DefaultAutoComplete
import xyz.nulldev.ts.api.http.serializer.DefaultCheckBox
import xyz.nulldev.ts.api.http.serializer.DefaultSelect
import xyz.nulldev.ts.api.http.serializer.DefaultSort
import xyz.nulldev.ts.api.http.serializer.DefaultText
import xyz.nulldev.ts.api.http.serializer.DefaultTriState
import xyz.nulldev.ts.api.http.serializer.FixtureAutoComplete
import xyz.nulldev.ts.api.http.serializer.FixtureCheckBox
import xyz.nulldev.ts.api.http.serializer.FixtureGroup
import xyz.nulldev.ts.api.http.serializer.FixtureSelect
import xyz.nulldev.ts.api.http.serializer.FixtureSort
import xyz.nulldev.ts.api.http.serializer.FixtureText
import xyz.nulldev.ts.api.http.serializer.FixtureTriState
import xyz.nulldev.ts.api.http.serializer.SkipOnlyAutoComplete

internal class FilterTest {
    @Test
    fun headerHoldsNameAndZeroState() {
        val header = Filter.Header("Head")
        header.name shouldBe "Head"
        header.state shouldBe 0
    }

    @Test
    fun separatorDefaultsToEmptyName() {
        Filter.Separator().name shouldBe ""
        Filter.Separator("Sep").name shouldBe "Sep"
        Filter.Separator().state shouldBe 0
    }

    @Test
    fun selectDefaultsToFirstOption() {
        val select = DefaultSelect("Sel", arrayOf("a", "b"))
        select.state shouldBe 0
        select.values.toList() shouldContainExactly listOf("a", "b")
        FixtureSelect("Sel", arrayOf(1, 2), 1).state shouldBe 1
    }

    @Test
    fun textDefaultsToEmpty() {
        DefaultText("Text").state shouldBe ""
        FixtureText("Text", "typed").state shouldBe "typed"
    }

    @Test
    fun checkBoxDefaultsToOff() {
        DefaultCheckBox("Box").state shouldBe false
        FixtureCheckBox("Box", true).state shouldBe true
    }

    @Test
    fun triStateDefaultsToIgnored() {
        DefaultTriState("Tri").state shouldBe Filter.TriState.STATE_IGNORE
        FixtureTriState("Tri", Filter.TriState.STATE_EXCLUDE).state shouldBe Filter.TriState.STATE_EXCLUDE
    }

    @Test
    fun triStateQueries() {
        val tri = DefaultTriState("Tri")
        tri.isIgnored() shouldBe true
        tri.isIncluded() shouldBe false
        tri.isExcluded() shouldBe false

        tri.state = Filter.TriState.STATE_INCLUDE
        tri.isIgnored() shouldBe false
        tri.isIncluded() shouldBe true
        tri.isExcluded() shouldBe false

        tri.state = Filter.TriState.STATE_EXCLUDE
        tri.isIgnored() shouldBe false
        tri.isIncluded() shouldBe false
        tri.isExcluded() shouldBe true
    }

    @Test
    fun groupHoldsNestedState() {
        val nested = DefaultCheckBox("A")
        val group = FixtureGroup("Group", listOf(nested, null))
        group.name shouldBe "Group"
        group.state shouldContainExactly listOf(nested, null)
    }

    @Test
    fun sortDefaultsToNoSelection() {
        val sort = DefaultSort("Sort", arrayOf("x", "y"))
        sort.state shouldBe null
        sort.values.toList() shouldContainExactly listOf("x", "y")
        FixtureSort("Sort", arrayOf("x"), Filter.Sort.Selection(0, true)).state shouldBe Filter.Sort.Selection(0, true)
    }

    @Test
    fun autoCompleteDefaults() {
        val plain = DefaultAutoComplete(name = "AC", hint = "hint", values = listOf("a"), state = listOf("b"))
        plain.hint shouldBe "hint"
        plain.values shouldContainExactly listOf("a")
        plain.state shouldContainExactly listOf("b")
        plain.skipAutoFillTags.shouldBeEmpty()
        plain.validPrefixes.shouldBeEmpty()

        val full = FixtureAutoComplete(
            name = "AC",
            hint = "hint",
            values = listOf("a"),
            skipAutoFillTags = listOf("skip"),
            validPrefixes = listOf("-"),
            state = emptyList(),
        )
        full.skipAutoFillTags shouldContainExactly listOf("skip")
        full.validPrefixes shouldContainExactly listOf("-")
        full.state.shouldBeEmpty()

        val skipOnly = SkipOnlyAutoComplete("AC", listOf("skip"))
        skipOnly.skipAutoFillTags shouldContainExactly listOf("skip")
        skipOnly.validPrefixes.shouldBeEmpty()
    }
}
