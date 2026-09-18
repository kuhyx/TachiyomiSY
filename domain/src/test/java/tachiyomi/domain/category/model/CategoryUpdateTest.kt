package tachiyomi.domain.category.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

internal class CategoryUpdateTest {

    private val full = CategoryUpdate(
        id = 4L,
        name = "Plan to read",
        order = 1L,
        flags = 0b100L,
        version = 2L,
        uid = 77L,
        lastModifiedAt = 500L,
    )

    @Test
    fun fieldsButIdDefaultToNull() {
        val update = CategoryUpdate(id = 4L)
        update.id shouldBe 4L
        update.name shouldBe null
        update.order shouldBe null
        update.flags shouldBe null
        update.version shouldBe null
        update.uid shouldBe null
        update.lastModifiedAt shouldBe null
        update.hashCode() shouldBe CategoryUpdate(id = 4L).hashCode()
        update.toString() shouldStartWith "CategoryUpdate(id=4, name=null, order=null, flags=null, "
        update.toString() shouldEndWith "version=null, uid=null, lastModifiedAt=null)"
    }

    @Test
    fun isADataClass() {
        val copy = full.copy(order = 9L)
        copy shouldNotBe full
        copy.order shouldBe 9L
        copy.copy(order = 1L) shouldBe full
        full.hashCode() shouldBe copy.copy(order = 1L).hashCode()
        full shouldNotBe CategoryUpdate(id = 4L)
        full.toString() shouldStartWith "CategoryUpdate(id=4, name=Plan to read, order=1, flags=4, "
        full.toString() shouldEndWith "version=2, uid=77, lastModifiedAt=500)"
    }

    @Test
    fun exposesEveryComponent() {
        val (id, name, order) = full
        id shouldBe 4L
        name shouldBe "Plan to read"
        order shouldBe 1L
        full.component4() shouldBe 0b100L
        full.component5() shouldBe 2L
        full.component6() shouldBe 77L
        full.component7() shouldBe 500L
    }
}
