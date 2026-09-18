package tachiyomi.domain.source.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class SourceTest {

    private val source = Source(id = 1L, lang = "en", name = "Src", supportsLatest = true, isStub = false)

    @Test
    fun defaultsAreMainRow() {
        source.pin shouldBe Pins.unpinned
        source.isUsedLast shouldBe false
        source.category shouldBe null
        source.isExcludedFromDataSaver shouldBe false
        source.categories shouldBe emptySet()
    }

    @Test
    fun visualNameWithLanguage() {
        source.visualName shouldBe "Src (EN)"
    }

    @Test
    fun visualNameWithoutLanguage() {
        source.copy(lang = "").visualName shouldBe "Src"
    }

    @Test
    fun keyOfMainRow() {
        source.key() shouldBe "1"
    }

    @Test
    fun keyOfLastUsedRow() {
        source.copy(isUsedLast = true, category = "Cat").key() shouldBe "1-lastused"
    }

    @Test
    fun keyOfCategoryRow() {
        source.copy(category = "Cat").key() shouldBe "1-Cat"
    }

    @Test
    fun explicitArguments() {
        val full = Source(
            id = 2L,
            lang = "ja",
            name = "Full",
            supportsLatest = false,
            isStub = true,
            pin = Pins.pinned,
            isUsedLast = true,
            category = "Cat",
            isExcludedFromDataSaver = true,
            categories = setOf("Cat"),
        )

        full.component1() shouldBe 2L
        full.component2() shouldBe "ja"
        full.component3() shouldBe "Full"
        full.component4() shouldBe false
        full.component5() shouldBe true
        full.component6() shouldBe Pins.pinned
        full.component7() shouldBe true
        full.component8() shouldBe "Cat"
        full.component9() shouldBe true
        full.component10() shouldBe setOf("Cat")
        full shouldBe full.copy()
        full shouldNotBe source
        full.hashCode() shouldBe full.copy().hashCode()
        full.toString() shouldBe "Source(id=2, lang=ja, name=Full, supportsLatest=false, isStub=true, " +
            "pin=Pins(code=3), isUsedLast=true, category=Cat, isExcludedFromDataSaver=true, categories=[Cat])"
    }

    @Test
    fun sourceWithCountDelegates() {
        val row = SourceWithCount(source = source, count = 4L)

        row.id shouldBe 1L
        row.name shouldBe "Src"
        row.component1() shouldBe source
        row.component2() shouldBe 4L
        row shouldBe row.copy()
        row.copy(count = 5L) shouldNotBe row
        row.hashCode() shouldBe row.copy().hashCode()
        row.toString() shouldBe "SourceWithCount(source=$source, count=4)"
    }
}
