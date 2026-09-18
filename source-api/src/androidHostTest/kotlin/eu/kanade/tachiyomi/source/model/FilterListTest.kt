package eu.kanade.tachiyomi.source.model

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import xyz.nulldev.ts.api.http.serializer.FixtureCheckBox
import xyz.nulldev.ts.api.http.serializer.FixtureText

internal class FilterListTest {
    private val text = FixtureText("T", "x")
    private val box = FixtureCheckBox("C", true)

    @Test
    fun varargBuildsList() {
        FilterList(text, box).list shouldContainExactly listOf(text, box)
    }

    @Test
    fun emptyVarargBuildsEmptyList() {
        val empty = FilterList()
        empty.list.shouldBeEmpty()
        empty.list shouldBe emptyList<Filter<*>>()
    }

    @Test
    fun primaryConstructorKeepsList() {
        val backing = listOf<Filter<*>>(text)
        FilterList(backing).list shouldBe backing
    }

    @Test
    fun delegatesToList() {
        val filters = FilterList(text, box)
        filters.size shouldBe 2
        filters[1] shouldBe box
        filters.contains(text) shouldBe true
        filters.isEmpty() shouldBe false
        filters.toList() shouldContainExactly listOf(text, box)
        filters.indexOf(box) shouldBe 1
    }

    @Test
    fun neverEquals() {
        val filters = FilterList(text)
        val same: Any = filters
        (filters == same) shouldBe false
        (filters == FilterList(text)) shouldBe false
        val nothing: Any? = null
        (filters == nothing) shouldBe false
    }

    @Test
    fun hashCodeMatchesList() {
        FilterList(text, box).hashCode() shouldBe listOf(text, box).hashCode()
    }

    @Test
    fun copyReplacesList() {
        FilterList(text).copy(list = listOf(box)).list shouldContainExactly listOf(box)
        FilterList(text).copy().list shouldContainExactly listOf(text)
    }

    @Test
    fun componentAndToString() {
        val filters = FilterList(text)
        val (list) = filters
        list shouldContainExactly listOf(text)
        filters.component1() shouldBe listOf(text)
        filters.toString() shouldStartWith "FilterList(list=["
    }
}
