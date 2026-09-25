package exh.uconfig

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class EhUConfigLanguagesTest {
    private val on = listOf(true, true, true)
    private val off = listOf(false, false, false)

    private fun keys(language: BaseLanguage) = language.configs.map { it.key to it.value }

    @Test
    fun japaneseHasNoOriginalRow() {
        keys(Japanese(on)) shouldContainExactly listOf("xl_1024" to "checked", "xl_2048" to "checked")
        keys(Japanese(off)) shouldContainExactly listOf("xl_1024" to "", "xl_2048" to "")
    }

    @Test
    fun translatedHaveThreeRows() {
        keys(English(listOf(true, false, true))) shouldContainExactly
            listOf("xl_1" to "checked", "xl_1025" to "", "xl_2049" to "checked")
        val all = listOf(
            Chinese(on), Dutch(on), French(on), German(on), Hungarian(on), Italian(on), Korean(on), Polish(on),
            Portuguese(on), Russian(on), Spanish(on), Thai(on), Vietnamese(on), NotAvailable(on), Other(on),
        )
        all.map { it.originalKey } shouldContainExactly listOf(
            "xl_10", "xl_20", "xl_30", "xl_40", "xl_50", "xl_60", "xl_70", "xl_80",
            "xl_90", "xl_100", "xl_110", "xl_120", "xl_130", "xl_254", "xl_255",
        )
        all.map { it.translatedKey } shouldContainExactly listOf(
            "xl_1034", "xl_1044", "xl_1054", "xl_1064", "xl_1074", "xl_1084", "xl_1094", "xl_1104",
            "xl_1114", "xl_1124", "xl_1134", "xl_1144", "xl_1154", "xl_1278", "xl_1279",
        )
        all.map { it.rewriteKey } shouldContainExactly listOf(
            "xl_2058", "xl_2068", "xl_2078", "xl_2088", "xl_2098", "xl_2108", "xl_2118", "xl_2128",
            "xl_2138", "xl_2148", "xl_2158", "xl_2168", "xl_2178", "xl_2302", "xl_2303",
        )
        all.forEach { it.configs.size shouldBe 3 }
    }

    @Test
    fun languageConfigItemValues() {
        BaseLanguage.LanguageConfigItem("k", true).value shouldBe "checked"
        BaseLanguage.LanguageConfigItem("k", false).value shouldBe ""
        BaseLanguage.LanguageConfigItem("k", false).key shouldBe "k"
    }

    @Test
    fun enumsKeepTheirOrder() {
        EhCategory.entries.map { it.configKey } shouldContainExactly listOf(
            "ct_doujinshi", "ct_manga", "ct_artistcg", "ct_gamecg", "ct_western",
            "ct_non-h", "ct_imageset", "ct_cosplay", "ct_asianporn", "ct_misc",
        )
        EhLanguage.entries.size shouldBe 17
        EhLanguage.entries.first() shouldBe EhLanguage.JAPANESE
        EhLanguage.entries.last() shouldBe EhLanguage.OTHER
    }

    @Test
    fun entryValuesAndKeys() {
        Entry.UseHentaiAtHome.entries.map { it.value } shouldContainExactly listOf("0", "1", "2")
        Entry.UseHentaiAtHome.NO.key shouldBe "uh"
        Entry.ImageSize.entries.map { it.value } shouldContainExactly listOf("0", "5", "4", "3", "2", "1")
        Entry.ImageSize.AUTO.key shouldBe "xr"
        Entry.TitleDisplayLanguage.JAPANESE.key shouldBe "tl"
        Entry.SearchResultsCount.entries.map { it.value } shouldContainExactly listOf("0", "1", "2", "3")
        Entry.SearchResultsCount.COUNT_25.key shouldBe "rc"
        Entry.ThumbnailRows.entries.map { it.value } shouldContainExactly listOf("0", "1", "2", "3")
        Entry.ThumbnailRows.ROWS_4.key shouldBe "tr"
        Entry.UseOriginalImages.YES.key shouldBe "oi"
        Entry.TagFilteringThreshold(-1).let {
            it.key shouldBe "ft"
            it.value shouldBe "-1"
        }
        Entry.TagWatchingThreshold(2).let {
            it.key shouldBe "wt"
            it.value shouldBe "2"
        }
    }

    @Test
    fun hathPerksToString() {
        EHHathPerksResponse().apply { thumbsUp = true }.toString() shouldBe
            "EHHathPerksResponse(moreThumbs=false, thumbsUp=true, allThumbs=false, " +
            "pagingEnlargementI=false, pagingEnlargementII=false, pagingEnlargementIII=false)"
    }
}
