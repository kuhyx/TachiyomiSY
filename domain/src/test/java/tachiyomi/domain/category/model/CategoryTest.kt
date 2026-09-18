package tachiyomi.domain.category.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

internal class CategoryTest {

    private val category = Category(
        id = 5L,
        name = "Reading",
        order = 2L,
        flags = 0b01000100L,
        version = 3L,
        uid = 99L,
        lastModifiedAt = 1_000L,
    )

    @Test
    fun syncFieldsDefaultToZero() {
        val fresh = Category(id = 1L, name = "n", order = 0L, flags = 0L)
        fresh.version shouldBe 0L
        fresh.uid shouldBe 0L
        fresh.lastModifiedAt shouldBe 0L
    }

    @Test
    fun onlyIdZeroIsSystemCategory() {
        Category.UNCATEGORIZED_ID shouldBe 0L
        Category(id = 0L, name = "", order = 0L, flags = 0L).isSystemCategory shouldBe true
        category.isSystemCategory shouldBe false
    }

    @Test
    fun isADataClass() {
        val copy = category.copy(name = "Done")
        copy shouldNotBe category
        copy.name shouldBe "Done"
        copy.copy(name = "Reading") shouldBe category
        copy.copy(name = "Reading").hashCode() shouldBe category.hashCode()
        category.toString() shouldStartWith "Category(id=5, name=Reading, order=2, flags=68, "
        category.toString() shouldEndWith "version=3, uid=99, lastModifiedAt=1000)"
    }

    @Test
    fun exposesEveryComponent() {
        val (id, name, order) = category
        id shouldBe 5L
        name shouldBe "Reading"
        order shouldBe 2L
        category.component4() shouldBe 0b01000100L
        category.component5() shouldBe 3L
        category.component6() shouldBe 99L
        category.component7() shouldBe 1_000L
    }

    @Test
    fun survivesJavaSerialization() {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(category) }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }

        restored shouldBe category
        (restored as Category).isSystemCategory shouldBe false
    }
}
