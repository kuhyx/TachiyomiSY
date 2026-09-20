package exh.uconfig

import exh.source.ExhPreferences
import okhttp3.FormBody
import uy.kohesive.injekt.injectLazy
import java.util.Locale

internal class EhUConfigBuilder {
    private val exhPreferences: ExhPreferences by injectLazy()

    fun build(hathPerks: EHHathPerksResponse): FormBody {
        val configItems = mutableListOf<ConfigItem>()

        configItems += when (
            exhPreferences.imageQuality
                .get()
                .lowercase(Locale.getDefault())
        ) {
            "ovrs_2400" -> Entry.ImageSize.PX_2400
            "ovrs_1600" -> Entry.ImageSize.PX_1600
            "high" -> Entry.ImageSize.PX_1280
            "med" -> Entry.ImageSize.PX_980
            "low" -> Entry.ImageSize.PX_780
            "auto" -> Entry.ImageSize.AUTO
            else -> Entry.ImageSize.AUTO
        }

        configItems += when (exhPreferences.useHentaiAtHome.get()) {
            2 -> Entry.UseHentaiAtHome.NO
            1 -> Entry.UseHentaiAtHome.DEFAULTONLY
            else -> Entry.UseHentaiAtHome.ANY
        }

        configItems += if (exhPreferences.useJapaneseTitle.get()) {
            Entry.TitleDisplayLanguage.JAPANESE
        } else {
            Entry.TitleDisplayLanguage.DEFAULT
        }

        configItems += if (exhPreferences.exhUseOriginalImages.get()) {
            Entry.UseOriginalImages.YES
        } else {
            Entry.UseOriginalImages.NO
        }

        configItems += when {
            hathPerks.allThumbs -> Entry.ThumbnailRows.ROWS_40
            hathPerks.thumbsUp -> Entry.ThumbnailRows.ROWS_20
            hathPerks.moreThumbs -> Entry.ThumbnailRows.ROWS_10
            else -> Entry.ThumbnailRows.ROWS_4
        }

        configItems += when {
            hathPerks.pagingEnlargementIII -> Entry.SearchResultsCount.COUNT_200
            hathPerks.pagingEnlargementII -> Entry.SearchResultsCount.COUNT_100
            hathPerks.pagingEnlargementI -> Entry.SearchResultsCount.COUNT_50
            else -> Entry.SearchResultsCount.COUNT_25
        }

        configItems += Entry.DisplayMode()
        configItems += Entry.UseMPV()
        configItems += Entry.ShowPopularRightNowPane()

        configItems += Entry.TagFilteringThreshold(exhPreferences.ehTagFilterValue.get())
        configItems += Entry.TagWatchingThreshold(exhPreferences.ehTagWatchingValue.get())

        configItems += Entry.LanguageSystem().getLanguages(exhPreferences.exhSettingsLanguages.get().split("\n"))

        configItems += Entry.Categories().categoryConfigs(
            exhPreferences.exhEnabledCategories.get().split(",").map {
                it.toBoolean()
            },
        )

        // Actually build form body
        val formBody = FormBody.Builder()
        configItems.forEach {
            formBody.add(it.key, it.value)
        }
        formBody.add("apply", "Apply")
        return formBody.build()
    }
}

internal object Entry {
    enum class UseHentaiAtHome(override val value: String) : ConfigItem {
        ANY("0"),
        DEFAULTONLY("1"),
        NO("2"),
        ;

        override val key = "uh"
    }

    enum class ImageSize(override val value: String) : ConfigItem {
        AUTO("0"),
        PX_2400("5"),
        PX_1600("4"),
        PX_1280("3"),
        PX_980("2"),
        PX_780("1"),
        ;

        override val key = "xr"
    }

    enum class TitleDisplayLanguage(override val value: String) : ConfigItem {
        DEFAULT("0"),
        JAPANESE("1"),
        ;

        override val key = "tl"
    }

    // Locked to extended mode as that's what the parser and toplists use
    class DisplayMode : ConfigItem {
        override val key = "dm"
        override val value = "2"
    }

    enum class SearchResultsCount(override val value: String) : ConfigItem {
        COUNT_25("0"),
        COUNT_50("1"),
        COUNT_100("2"),
        COUNT_200("3"),
        ;

        override val key = "rc"
    }

    enum class ThumbnailRows(override val value: String) : ConfigItem {
        ROWS_4("0"),
        ROWS_10("1"),
        ROWS_20("2"),
        ROWS_40("3"),
        ;

        override val key = "tr"
    }

    enum class UseOriginalImages(override val value: String) : ConfigItem {
        NO("0"),
        YES("1"),
        ;

        override val key = "oi"
    }

    // Locked to no MPV as that's what the parser uses
    class UseMPV : ConfigItem {
        override val key = "qb"
        override val value = "0"
    }

    // Locked to no popular pane as we can't parse it
    class ShowPopularRightNowPane : ConfigItem {
        override val key = "pp"
        override val value = "1"
    }

    class TagFilteringThreshold(value: Int) : ConfigItem {
        override val key = "ft"
        override val value = "$value"
    }

    class TagWatchingThreshold(value: Int) : ConfigItem {
        override val key = "wt"
        override val value = "$value"
    }

    class Categories {

        fun categoryConfigs(list: List<Boolean>): List<ConfigItem> =
            EhCategory.entries.map { GenreConfigItem(it.configKey, list[it.ordinal]) }

        private class GenreConfigItem(override val key: String, exclude: Boolean) : ConfigItem {
            override val value = if (exclude) "1" else "0"
        }
    }

    class LanguageSystem {
        private fun transformConfig(values: List<String>) = values.map { pref ->
            pref.split("*").map { it.toBoolean() }
        }

        fun getLanguages(values: List<String>): List<ConfigItem> {
            val config = transformConfig(values)
            return EhLanguage.entries.flatMap { languageOf.getValue(it)(config[it.ordinal]).configs }
        }

        private val languageOf: Map<EhLanguage, (List<Boolean>) -> BaseLanguage> = mapOf(
            EhLanguage.JAPANESE to ::Japanese,
            EhLanguage.ENGLISH to ::English,
            EhLanguage.CHINESE to ::Chinese,
            EhLanguage.DUTCH to ::Dutch,
            EhLanguage.FRENCH to ::French,
            EhLanguage.GERMAN to ::German,
            EhLanguage.HUNGARIAN to ::Hungarian,
            EhLanguage.ITALIAN to ::Italian,
            EhLanguage.KOREAN to ::Korean,
            EhLanguage.POLISH to ::Polish,
            EhLanguage.PORTUGUESE to ::Portuguese,
            EhLanguage.RUSSIAN to ::Russian,
            EhLanguage.SPANISH to ::Spanish,
            EhLanguage.THAI to ::Thai,
            EhLanguage.VIETNAMESE to ::Vietnamese,
            EhLanguage.NOT_AVAILABLE to ::NotAvailable,
            EhLanguage.OTHER to ::Other,
        )

        private abstract class BaseLanguage(val values: List<Boolean>) {
            abstract val translatedKey: String
            abstract val rewriteKey: String

            open val configs: List<LanguageConfigItem>
                get() = listOf(
                    LanguageConfigItem(translatedKey, values[1]),
                    LanguageConfigItem(rewriteKey, values[2]),
                )

            protected class LanguageConfigItem(override val key: String, value: Boolean) : ConfigItem {
                override val value = if (value) "checked" else ""
            }
        }

        private abstract class Language(values: List<Boolean>) : BaseLanguage(values) {
            abstract val originalKey: String

            override val configs: List<LanguageConfigItem>
                get() = listOf(
                    LanguageConfigItem(originalKey, values[0]),
                    LanguageConfigItem(translatedKey, values[1]),
                    LanguageConfigItem(rewriteKey, values[2]),
                )
        }

        private class Japanese(values: List<Boolean>) : BaseLanguage(values) {
            override val translatedKey: String = "xl_1024"
            override val rewriteKey: String = "xl_2048"
        }

        private class English(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_1"
            override val translatedKey: String = "xl_1025"
            override val rewriteKey: String = "xl_2049"
        }

        private class Chinese(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_10"
            override val translatedKey: String = "xl_1034"
            override val rewriteKey: String = "xl_2058"
        }

        private class Dutch(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_20"
            override val translatedKey: String = "xl_1044"
            override val rewriteKey: String = "xl_2068"
        }

        private class French(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_30"
            override val translatedKey: String = "xl_1054"
            override val rewriteKey: String = "xl_2078"
        }

        private class German(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_40"
            override val translatedKey: String = "xl_1064"
            override val rewriteKey: String = "xl_2088"
        }

        private class Hungarian(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_50"
            override val translatedKey: String = "xl_1074"
            override val rewriteKey: String = "xl_2098"
        }

        private class Italian(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_60"
            override val translatedKey: String = "xl_1084"
            override val rewriteKey: String = "xl_2108"
        }

        private class Korean(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_70"
            override val translatedKey: String = "xl_1094"
            override val rewriteKey: String = "xl_2118"
        }

        private class Polish(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_80"
            override val translatedKey: String = "xl_1104"
            override val rewriteKey: String = "xl_2128"
        }

        private class Portuguese(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_90"
            override val translatedKey: String = "xl_1114"
            override val rewriteKey: String = "xl_2138"
        }

        private class Russian(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_100"
            override val translatedKey: String = "xl_1124"
            override val rewriteKey: String = "xl_2148"
        }

        private class Spanish(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_110"
            override val translatedKey: String = "xl_1134"
            override val rewriteKey: String = "xl_2158"
        }

        private class Thai(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_120"
            override val translatedKey: String = "xl_1144"
            override val rewriteKey: String = "xl_2168"
        }

        private class Vietnamese(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_130"
            override val translatedKey: String = "xl_1154"
            override val rewriteKey: String = "xl_2178"
        }

        private class NotAvailable(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_254"
            override val translatedKey: String = "xl_1278"
            override val rewriteKey: String = "xl_2302"
        }

        private class Other(values: List<Boolean>) : Language(values) {
            override val originalKey: String = "xl_255"
            override val translatedKey: String = "xl_1279"
            override val rewriteKey: String = "xl_2303"
        }
    }
}

internal interface ConfigItem {
    val key: String
    val value: String
}
