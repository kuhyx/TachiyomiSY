package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import exh.uconfig.EhLanguage

internal class LanguageDialogState(preference: String) {
    class RowState(original: ColumnState, translated: ColumnState, rewrite: ColumnState) {
        var original by mutableStateOf(original)
        var translated by mutableStateOf(translated)
        var rewrite by mutableStateOf(rewrite)

        fun toPreference() = "${original.value}*${translated.value}*${rewrite.value}"
    }
    enum class ColumnState(val value: String) {
        Unavailable("false"),
        Enabled("true"),
        Disabled("false"),
    }
    val japanese: RowState
    val english: RowState
    val chinese: RowState
    val dutch: RowState
    val french: RowState
    val german: RowState
    val hungarian: RowState
    val italian: RowState
    val korean: RowState
    val polish: RowState
    val portuguese: RowState
    val russian: RowState
    val spanish: RowState
    val thai: RowState
    val vietnamese: RowState
    val notAvailable: RowState
    val other: RowState

    init {
        val settingsLanguages = preference.split("\n")
        japanese = settingsLanguages[EhLanguage.JAPANESE.ordinal].toRowState(true)
        english = settingsLanguages[EhLanguage.ENGLISH.ordinal].toRowState()
        chinese = settingsLanguages[EhLanguage.CHINESE.ordinal].toRowState()
        dutch = settingsLanguages[EhLanguage.DUTCH.ordinal].toRowState()
        french = settingsLanguages[EhLanguage.FRENCH.ordinal].toRowState()
        german = settingsLanguages[EhLanguage.GERMAN.ordinal].toRowState()
        hungarian = settingsLanguages[EhLanguage.HUNGARIAN.ordinal].toRowState()
        italian = settingsLanguages[EhLanguage.ITALIAN.ordinal].toRowState()
        korean = settingsLanguages[EhLanguage.KOREAN.ordinal].toRowState()
        polish = settingsLanguages[EhLanguage.POLISH.ordinal].toRowState()
        portuguese = settingsLanguages[EhLanguage.PORTUGUESE.ordinal].toRowState()
        russian = settingsLanguages[EhLanguage.RUSSIAN.ordinal].toRowState()
        spanish = settingsLanguages[EhLanguage.SPANISH.ordinal].toRowState()
        thai = settingsLanguages[EhLanguage.THAI.ordinal].toRowState()
        vietnamese = settingsLanguages[EhLanguage.VIETNAMESE.ordinal].toRowState()
        notAvailable = settingsLanguages[EhLanguage.NOT_AVAILABLE.ordinal].toRowState()
        other = settingsLanguages[EhLanguage.OTHER.ordinal].toRowState()
    }

    private fun String.toRowState(disableFirst: Boolean = false) = split("*")
        .map {
            if (it.toBoolean()) {
                ColumnState.Enabled
            } else {
                ColumnState.Disabled
            }
        }
        .let {
            if (disableFirst) {
                RowState(ColumnState.Unavailable, it[1], it[2])
            } else {
                RowState(it[0], it[1], it[2])
            }
        }

    fun toPreference() = listOf(
        japanese,
        english,
        chinese,
        dutch,
        french,
        german,
        hungarian,
        italian,
        korean,
        polish,
        portuguese,
        russian,
        spanish,
        thai,
        vietnamese,
        notAvailable,
        other,
    ).joinToString("\n") { it.toPreference() }
}
