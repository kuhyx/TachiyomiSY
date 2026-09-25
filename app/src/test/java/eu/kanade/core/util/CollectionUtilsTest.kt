package eu.kanade.core.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class CollectionUtilsTest {

    @Test
    fun insertSeparatorsWrapsEveryGap() {
        emptyList<String>().insertSeparators { _, _ -> "x" } shouldBe emptyList()
        listOf("a", "b").insertSeparators { before, after -> "$before>$after" } shouldBe
            listOf("null>a", "a", "a>b", "b", "b>null")
        val innerOnly = listOf("a", "b").insertSeparators { before, after ->
            before?.let { after?.let { "|" } }
        }
        innerOnly shouldBe listOf("a", "|", "b")
    }

    @Test
    fun insertSeparatorsReversedKeeps() {
        emptyList<String>().insertSeparatorsReversed { _, _ -> "x" } shouldBe emptyList()
        listOf("a", "b").insertSeparatorsReversed { before, after -> "$before>$after" } shouldBe
            listOf("null>a", "a", "a>b", "b", "b>null")
        listOf("a", "b").insertSeparatorsReversed { before, _ -> before?.let { "|" } } shouldBe
            listOf("a", "|", "b", "|")
    }

    @Test
    fun addOrRemove() {
        val set = hashSetOf(1)
        set.addOrRemove(2, shouldAdd = true)
        set.addOrRemove(1, shouldAdd = false)
        set shouldBe setOf(2)
    }

    @Test
    fun fastFilterNotDropsMatches() {
        listOf(1, 2, 3, 4).fastFilterNot { it % 2 == 0 } shouldBe listOf(1, 3)
        emptyList<Int>().fastFilterNot { true } shouldBe emptyList()
    }

    @Test
    fun fastPartitionSplitsByPredicate() {
        listOf(1, 2, 3, 4).fastPartition { it > 2 } shouldBe (listOf(3, 4) to listOf(1, 2))
    }

    @Test
    fun fastCountNotCountsMisses() {
        listOf(1, 2, 3, 4).fastCountNot { it > 2 } shouldBe 2
        emptyList<Int>().fastCountNot { true } shouldBe 0
    }

    // The inline bodies only run when called as plain methods, which reflection can do.
    @Test
    fun compiledInlineBodiesAgree() {
        val utils = Class.forName("eu.kanade.core.util.CollectionUtilsKt")
        val even: (Int) -> Boolean = { it % 2 == 0 }
        val list = listOf(1, 2, 3, 4)
        utils.getDeclaredMethod("fastFilterNot", List::class.java, Function1::class.java)
            .invoke(null, list, even) shouldBe listOf(1, 3)
        utils.getDeclaredMethod("fastPartition", List::class.java, Function1::class.java)
            .invoke(null, list, even) shouldBe (listOf(2, 4) to listOf(1, 3))
        utils.getDeclaredMethod("fastCountNot", List::class.java, Function1::class.java)
            .invoke(null, list, even) shouldBe 2
    }
}
