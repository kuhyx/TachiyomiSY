package eu.kanade.tachiyomi.source.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import xyz.nulldev.ts.api.http.serializer.DefaultSort
import xyz.nulldev.ts.api.http.serializer.DefaultText
import xyz.nulldev.ts.api.http.serializer.FixtureCheckBox
import xyz.nulldev.ts.api.http.serializer.FixtureText
import xyz.nulldev.ts.api.http.serializer.FixtureTriState

internal class FilterEqualityTest {
    private val text = FixtureText("Title", "abc")

    @Test
    fun sameInstanceIsEqual() {
        val same: Filter<*> = text
        (text == same) shouldBe true
    }

    @Test
    fun otherTypeIsNotEqual() {
        val other: Any = "Title"
        (text == other) shouldBe false
    }

    @Test
    fun differentNameIsNotEqual() {
        (text == FixtureText("Other", "abc")) shouldBe false
    }

    @Test
    fun differentStateIsNotEqual() {
        (text == FixtureText("Title", "xyz")) shouldBe false
    }

    @Test
    fun sameNameAndStateAreEqual() {
        (text == FixtureText("Title", "abc")) shouldBe true
        text shouldBe FixtureText("Title", "abc")
    }

    @Test
    fun differentStateTypesAreNotEqual() {
        val toggle: Filter<*> = FixtureCheckBox("Title", true)
        val tri: Filter<*> = FixtureTriState("Title", 1)
        (toggle == tri) shouldBe false
        (text == toggle) shouldBe false
    }

    @Test
    fun hashCodeUsesNameAndState() {
        text.hashCode() shouldBe 31 * "Title".hashCode() + "abc".hashCode()
        DefaultText("Title").hashCode() shouldBe 31 * "Title".hashCode() + "".hashCode()
    }

    @Test
    fun hashCodeWithNullState() {
        DefaultSort("Sort", arrayOf("a")).hashCode() shouldBe 31 * "Sort".hashCode()
    }

    @Test
    fun selectionIsDataClass() {
        val selection = Filter.Sort.Selection(1, true)
        selection.index shouldBe 1
        selection.ascending shouldBe true
        selection.component1() shouldBe 1
        selection.component2() shouldBe true
        selection.copy(ascending = false) shouldBe Filter.Sort.Selection(1, false)
        selection.copy() shouldBe selection
        selection.hashCode() shouldBe Filter.Sort.Selection(1, true).hashCode()
        selection.toString() shouldBe "Selection(index=1, ascending=true)"
        selection shouldNotBe Filter.Sort.Selection(2, true)
    }
}
