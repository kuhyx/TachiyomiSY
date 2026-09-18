package tachiyomi.core.common.preference

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

private const val FACADE = "tachiyomi.core.common.preference.CheckboxStateKt"

internal class CheckboxStateTest {
    @Test
    fun stateNextFlipsChecked() {
        val checked: CheckboxState.State<String> = CheckboxState.State.Checked("a")
        val none: CheckboxState.State<String> = CheckboxState.State.None("a")
        checked.isChecked shouldBe true
        none.isChecked shouldBe false
        checked.next() shouldBe none
        none.next() shouldBe checked
        val base: CheckboxState<String> = checked
        base.value shouldBe "a"
    }

    @Test
    fun triStateNextCycles() {
        val none: CheckboxState.TriState<Int> = CheckboxState.TriState.None(1)
        val include: CheckboxState.TriState<Int> = CheckboxState.TriState.Include(1)
        val exclude: CheckboxState.TriState<Int> = CheckboxState.TriState.Exclude(1)
        none.next() shouldBe include
        include.next() shouldBe exclude
        exclude.next() shouldBe none
        include.value shouldBe 1
        exclude.value shouldBe 1
        none.value shouldBe 1
    }

    @Test
    fun stateDataClassMembers() {
        val checked = CheckboxState.State.Checked("x")
        checked.copy(value = "y") shouldBe CheckboxState.State.Checked("y")
        checked.component1() shouldBe "x"
        checked.hashCode() shouldBe CheckboxState.State.Checked("x").hashCode()
        checked.toString() shouldBe "Checked(value=x)"
        checked shouldNotBe CheckboxState.State.None("x")

        val none = CheckboxState.State.None("x")
        none.copy(value = "z") shouldBe CheckboxState.State.None("z")
        none.component1() shouldBe "x"
        none.hashCode() shouldBe CheckboxState.State.None("x").hashCode()
        none.toString() shouldBe "None(value=x)"
        none shouldNotBe checked
    }

    @Test
    fun triStateDataClassMembers() {
        val include = CheckboxState.TriState.Include(1)
        val exclude = CheckboxState.TriState.Exclude(1)
        val none = CheckboxState.TriState.None(1)
        include.copy(value = 2) shouldBe CheckboxState.TriState.Include(2)
        exclude.copy(value = 2) shouldBe CheckboxState.TriState.Exclude(2)
        none.copy(value = 2) shouldBe CheckboxState.TriState.None(2)
        include.component1() shouldBe 1
        exclude.component1() shouldBe 1
        none.component1() shouldBe 1
        include.toString() shouldBe "Include(value=1)"
        exclude.toString() shouldBe "Exclude(value=1)"
        none.toString() shouldBe "None(value=1)"
        include.hashCode() shouldBe CheckboxState.TriState.Include(1).hashCode()
        exclude.hashCode() shouldBe CheckboxState.TriState.Exclude(1).hashCode()
        none.hashCode() shouldBe CheckboxState.TriState.None(1).hashCode()
        include shouldNotBe exclude
        exclude shouldNotBe none
        none shouldNotBe include
    }

    @Test
    fun asCheckboxStateInlined() {
        3.asCheckboxState { it > 2 } shouldBe CheckboxState.State.Checked(3)
        1.asCheckboxState { it > 2 } shouldBe CheckboxState.State.None(1)
        listOf(1, 3).mapAsCheckboxState { it > 2 } shouldBe listOf(
            CheckboxState.State.None(1),
            CheckboxState.State.Checked(3),
        )
    }

    @Test
    fun asCheckboxNonInlined() {
        val method = staticMethod(FACADE, "asCheckboxState", listOf(Any::class.java, Function1::class.java))
        val isBig: (Int) -> Boolean = { it > 2 }
        method.callStatic(listOf(3, isBig)) shouldBe CheckboxState.State.Checked(3)
        method.callStatic(listOf(1, isBig)) shouldBe CheckboxState.State.None(1)
    }

    @Test
    fun mapAsCheckboxNonInlined() {
        val method = staticMethod(FACADE, "mapAsCheckboxState", listOf(List::class.java, Function1::class.java))
        val isBig: (Int) -> Boolean = { it > 2 }
        val mapped = method.callStatic(listOf(listOf(1, 3), isBig)).shouldBeInstanceOf<List<*>>()
        mapped shouldBe listOf(CheckboxState.State.None(1), CheckboxState.State.Checked(3))
    }
}
