package exh.uconfig

import exh.source.ExhPreferences
import okhttp3.FormBody
import uy.kohesive.injekt.injectLazy
import java.util.Locale

internal class EhUConfigBuilder {
    private val exhPreferences: ExhPreferences by injectLazy()

    fun build(hathPerks: EHHathPerksResponse): FormBody {
        val configItems = preferenceEntries() + perkEntries(hathPerks) + fixedEntries()

        // Actually build form body
        val formBody = FormBody.Builder()
        configItems.forEach {
            formBody.add(it.key, it.value)
        }
        formBody.add("apply", "Apply")
        return formBody.build()
    }

    // The entries the user's preferences decide.
    private fun preferenceEntries(): List<ConfigItem> = listOf(
        when (exhPreferences.imageQuality.get().lowercase(Locale.getDefault())) {
            "ovrs_2400" -> Entry.ImageSize.PX_2400
            "ovrs_1600" -> Entry.ImageSize.PX_1600
            "high" -> Entry.ImageSize.PX_1280
            "med" -> Entry.ImageSize.PX_980
            "low" -> Entry.ImageSize.PX_780
            "auto" -> Entry.ImageSize.AUTO
            else -> Entry.ImageSize.AUTO
        },
        when (exhPreferences.useHentaiAtHome.get()) {
            2 -> Entry.UseHentaiAtHome.NO
            1 -> Entry.UseHentaiAtHome.DEFAULTONLY
            else -> Entry.UseHentaiAtHome.ANY
        },
        if (exhPreferences.useJapaneseTitle.get()) {
            Entry.TitleDisplayLanguage.JAPANESE
        } else {
            Entry.TitleDisplayLanguage.DEFAULT
        },
        if (exhPreferences.exhUseOriginalImages.get()) {
            Entry.UseOriginalImages.YES
        } else {
            Entry.UseOriginalImages.NO
        },
        Entry.TagFilteringThreshold(exhPreferences.ehTagFilterValue.get()),
        Entry.TagWatchingThreshold(exhPreferences.ehTagWatchingValue.get()),
    ) + Entry.LanguageSystem().getLanguages(exhPreferences.exhSettingsLanguages.get().split("\n")) +
        Entry.Categories().categoryConfigs(exhPreferences.exhEnabledCategories.get().split(",").map { it.toBoolean() })

    // The entries the account's Hath perks unlock.
    private fun perkEntries(hathPerks: EHHathPerksResponse): List<ConfigItem> = listOf(
        when {
            hathPerks.allThumbs -> Entry.ThumbnailRows.ROWS_40
            hathPerks.thumbsUp -> Entry.ThumbnailRows.ROWS_20
            hathPerks.moreThumbs -> Entry.ThumbnailRows.ROWS_10
            else -> Entry.ThumbnailRows.ROWS_4
        },
        when {
            hathPerks.pagingEnlargementIII -> Entry.SearchResultsCount.COUNT_200
            hathPerks.pagingEnlargementII -> Entry.SearchResultsCount.COUNT_100
            hathPerks.pagingEnlargementI -> Entry.SearchResultsCount.COUNT_50
            else -> Entry.SearchResultsCount.COUNT_25
        },
    )

    private fun fixedEntries(): List<ConfigItem> = listOf(
        Entry.DisplayMode(),
        Entry.UseMPV(),
        Entry.ShowPopularRightNowPane(),
    )
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

        private fun transformConfig(values: List<String>) = values.map { pref ->
            pref.split("*").map { it.toBoolean() }
        }

        fun getLanguages(values: List<String>): List<ConfigItem> {
            val config = transformConfig(values)
            return EhLanguage.entries.flatMap { languageOf.getValue(it)(config[it.ordinal]).configs }
        }
    }
}

internal interface ConfigItem {
    val key: String
    val value: String
}
