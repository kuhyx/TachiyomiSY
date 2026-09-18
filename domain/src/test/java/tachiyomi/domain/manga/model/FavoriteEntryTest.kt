package tachiyomi.domain.manga.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class FavoriteEntryTest {

    private val entry = FavoriteEntry(
        title = "Title",
        gid = "123",
        token = "abc",
        otherGid = "456",
        otherToken = "def",
        category = 3,
    )

    @Test
    fun defaultsAreEmpty() {
        val plain = FavoriteEntry(title = "Title", gid = "123", token = "abc")

        plain.otherGid shouldBe null
        plain.otherToken shouldBe null
        plain.category shouldBe -1
    }

    @Test
    fun components() {
        val (title, gid, token) = entry

        title shouldBe "Title"
        gid shouldBe "123"
        token shouldBe "abc"
        entry.component4() shouldBe "456"
        entry.component5() shouldBe "def"
        entry.component6() shouldBe 3
    }

    @Test
    fun dataClassMembers() {
        entry.copy() shouldBe entry
        entry.copy().hashCode() shouldBe entry.hashCode()
        entry.toString() shouldBe
            "FavoriteEntry(title=Title, gid=123, token=abc, otherGid=456, otherToken=def, category=3)"
        (entry == entry.copy(category = 4)) shouldBe false
    }

    @Test
    fun urlIsBuiltFromGidAndToken() {
        entry.getUrl() shouldBe "/g/123/abc/?nw=always"
    }

    @Test
    fun alternativeMembers() {
        val alternative = FavoriteEntryAlternative(otherGid = "1", otherToken = "2", gid = "3", token = "4")
        val (otherGid, otherToken, gid) = alternative

        otherGid shouldBe "1"
        otherToken shouldBe "2"
        gid shouldBe "3"
        alternative.component4() shouldBe "4"
        alternative.copy() shouldBe alternative
        alternative.copy().hashCode() shouldBe alternative.hashCode()
        alternative.toString() shouldBe "FavoriteEntryAlternative(otherGid=1, otherToken=2, gid=3, token=4)"
        (alternative == alternative.copy(token = "5")) shouldBe false
    }
}
