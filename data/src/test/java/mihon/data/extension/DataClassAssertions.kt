package mihon.data.extension

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Exercises the generated `equals`/`hashCode`/`toString` of a data class: [value] equals itself
 * and [equal] (a distinct instance with the same fields), differs from every entry of
 * [different] (each varying one field) and from an unrelated object.
 */
internal fun <T : Any> assertDataClass(value: T, equal: T, different: List<T>) {
    val unrelated: Any = "unrelated"
    value shouldBe value
    value shouldBe equal
    value.hashCode() shouldBe equal.hashCode()
    value.toString() shouldBe equal.toString()
    (value == unrelated) shouldBe false
    different.forEach { it shouldNotBe value }
}
