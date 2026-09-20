package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.core.os.LocaleListCompat
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.util.Locale

private const val ZH_CN = "zh-CN"
private const val ZH_TW = "zh-TW"

/**
 * Utility class to change the application's language in runtime.
 */
internal object LocaleHelper {

    /**
     * Sorts by display name, except keeps the "all" (displayed as "Multi") locale at the top.
     */
    val comparator = { a: String, b: String ->
        if (a == "all") {
            -1
        } else if (b == "all") {
            1
        } else {
            getLocalizedDisplayName(a).compareTo(getLocalizedDisplayName(b))
        }
    }

    /**
     * Returns display name of a string language code.
     */
    fun getSourceDisplayName(lang: String?, context: Context): String {
        // SY -->
        if (lang != null && lang.contains("custom|")) {
            return lang.split("|")[1]
        }
        // SY <--
        return when (lang) {
            SourcesScreenModel.LAST_USED_KEY -> context.stringResource(MR.strings.last_used_source)
            SourcesScreenModel.PINNED_KEY -> context.stringResource(MR.strings.pinned_sources)
            "other" -> context.stringResource(MR.strings.other_source)
            "all" -> context.stringResource(MR.strings.multi_lang)
            else -> getLocalizedDisplayName(lang)
        }
    }

    fun getDisplayName(lang: String): String {
        val normalizedLang = when (lang) {
            ZH_CN -> "zh-Hans"
            ZH_TW -> "zh-Hant"
            else -> lang
        }

        return Locale.forLanguageTag(normalizedLang).displayName
    }

    fun getShortDisplayName(lang: String?, uppercase: Boolean = false): String {
        return when (lang) {
            null -> ""
            "es-419" -> "es-la"
            ZH_CN -> "zh-hans"
            ZH_TW -> "zh-hant"
            else -> lang
        }
            .let { if (uppercase) it.uppercase(Locale.ENGLISH) else it }
    }

    /**
     * Returns display name of a string language code.
     *
     * @param lang empty for system language
     */
    fun getLocalizedDisplayName(lang: String?): String {
        if (lang == null) {
            return ""
        }

        val locale = when (lang) {
            "" -> LocaleListCompat.getAdjustedDefault()[0]
            ZH_CN -> Locale.forLanguageTag("zh-Hans")
            ZH_TW -> Locale.forLanguageTag("zh-Hant")
            else -> Locale.forLanguageTag(lang)
        }
        return locale!!.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
    }

    /**
     * Return the default languages enabled for the sources.
     */
    fun getDefaultEnabledLanguages(): Set<String> = setOf("all", "en", Locale.getDefault().language)
}
