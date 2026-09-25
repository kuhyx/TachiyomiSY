package eu.kanade.core.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** A selectable row for the range-selection tests. */
private data class Row(val id: Int, val selected: Boolean = false)

internal class RangeSelectionTest {

    private val positions = arrayOf(-1, -1)
    private val tracked = mutableListOf<Pair<Int, Boolean>>()

    private fun rows(vararg selected: Int): MutableList<Row> = MutableList(6) { Row(it, it in selected) }

    private fun MutableList<Row>.toggle(index: Int, selected: Boolean, fromLongPress: Boolean = false) = toggleRow(
        selectedIndex = index,
        selected = selected,
        fromLongPress = fromLongPress,
        positions = positions,
        isSelected = { it.selected },
        withSelected = { row, value -> row.copy(selected = value) },
        track = { row, value -> tracked += row.id to value },
    )

    @Test
    fun ignoresMissingAndUnchangedRows() {
        val list = rows(2)
        list.toggle(-1, selected = true)
        list.toggle(2, selected = true)
        list.toggle(3, selected = false)
        tracked shouldBe emptyList()
        positions.toList() shouldBe listOf(-1, -1)
    }

    @Test
    fun tapsMoveTheRangeEnds() {
        val list = rows()
        list.toggle(2, selected = true, fromLongPress = true)
        positions.toList() shouldBe listOf(2, 2)
        list.toggle(4, selected = true)
        positions.toList() shouldBe listOf(2, 4)
        list.toggle(0, selected = true)
        positions.toList() shouldBe listOf(0, 4)
        list.toggle(5, selected = true)
        positions.toList() shouldBe listOf(0, 5)
        list.toggle(3, selected = true)
        positions.toList() shouldBe listOf(0, 5)
        list.toggle(0, selected = false)
        positions.toList() shouldBe listOf(2, 5)
        list.toggle(5, selected = false)
        positions.toList() shouldBe listOf(2, 4)
        list.toggle(3, selected = false)
        positions.toList() shouldBe listOf(2, 4)
        list.toggle(2, selected = false)
        positions.toList() shouldBe listOf(4, 4)
        list.toggle(4, selected = false)
        positions.toList() shouldBe listOf(-1, 4)
        tracked.size shouldBe 10
        list.none { it.selected } shouldBe true
    }

    @Test
    fun firstTapOnlyMovesTheUpperEnd() {
        val list = rows()
        list.toggle(3, selected = true)
        positions.toList() shouldBe listOf(-1, 3)
    }

    @Test
    fun longPressExtendsTheRange() {
        val list = rows()
        list.toggle(2, selected = true, fromLongPress = true)
        positions.toList() shouldBe listOf(2, 2)
        list.toggle(5, selected = true, fromLongPress = true)
        positions.toList() shouldBe listOf(2, 5)
        list.map { it.selected } shouldBe listOf(false, false, true, true, true, true)
        list.toggle(0, selected = true, fromLongPress = true)
        positions.toList() shouldBe listOf(0, 5)
        list.all { it.selected } shouldBe true
        tracked.map { it.first } shouldBe listOf(2, 5, 3, 4, 0, 1)
    }

    @Test
    fun longPressInsideRangeSelects() {
        val list = rows(1, 4)
        positions[0] = 1
        positions[1] = 4
        list.toggle(2, selected = true, fromLongPress = true)
        positions.toList() shouldBe listOf(1, 4)
        list.map { it.selected } shouldBe listOf(false, true, true, false, true, false)
        list.toggle(1, selected = false, fromLongPress = true)
        tracked shouldBe listOf(2 to true, 1 to false)
        positions.toList() shouldBe listOf(1, 4)
    }

    @Test
    fun longPressSkipsSelectedRows() {
        val list = rows(1, 3, 4)
        positions[0] = 1
        positions[1] = 3
        list.toggle(5, selected = true, fromLongPress = true)
        tracked shouldBe listOf(5 to true)
        list.map { it.selected } shouldBe listOf(false, true, false, true, true, true)
    }
}
