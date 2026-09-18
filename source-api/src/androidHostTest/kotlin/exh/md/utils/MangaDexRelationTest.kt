package exh.md.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.sy.SYMR

internal class MangaDexRelationTest {
    @Test
    fun fromDexFindsKnownValue() {
        MangaDexRelation.fromDex("sequel") shouldBe MangaDexRelation.SEQUEL
        MangaDexRelation.fromDex("alternate_version") shouldBe MangaDexRelation.ALTERNATE_VERSION
    }

    @Test
    fun fromDexIsNullForUnknown() {
        MangaDexRelation.fromDex("unknown") shouldBe null
        MangaDexRelation.fromDex("") shouldBe null
    }

    @Test
    fun similarHasNoDexValue() {
        MangaDexRelation.SIMILAR.mdString shouldBe null
        MangaDexRelation.SIMILAR.res shouldBe SYMR.strings.relation_similar
    }

    @Test
    fun everyOtherValueIsUnique() {
        val dexValues = MangaDexRelation.entries.mapNotNull { it.mdString }
        dexValues.size shouldBe MangaDexRelation.entries.size - 1
        dexValues.toSet().size shouldBe dexValues.size
        dexValues.forEach { value -> MangaDexRelation.fromDex(value)?.mdString shouldBe value }
    }

    @Test
    fun entriesMatchValues() {
        MangaDexRelation.entries.size shouldBe 17
        MangaDexRelation.entries.first() shouldBe MangaDexRelation.SIMILAR
        MangaDexRelation.entries.last() shouldBe MangaDexRelation.ALTERNATE_VERSION
        MangaDexRelation.valueOf("MONOCHROME") shouldBe MangaDexRelation.MONOCHROME
        MangaDexRelation.MONOCHROME.res shouldBe SYMR.strings.relation_monochrome
        MangaDexRelation.MONOCHROME.mdString shouldBe "monochrome"
    }
}
