package eu.kanade.tachiyomi.ui.reader.setting

import tachiyomi.core.common.preference.Preference

// Split two-page spread.
// region Split two-page spread

internal val ReaderPreferences.dualPageSplitPaged: Preference<Boolean>
    get() = preferenceStore.getBoolean("pref_dual_page_split", false)

internal val ReaderPreferences.dualPageInvertPaged: Preference<Boolean>
    get() = preferenceStore.getBoolean("pref_dual_page_invert", false)

internal val ReaderPreferences.dualPageSplitWebtoon: Preference<Boolean>
    get() = preferenceStore.getBoolean("pref_dual_page_split_webtoon", false)

internal val ReaderPreferences.dualPageInvertWebtoon: Preference<Boolean>
    get() = preferenceStore.getBoolean("pref_dual_page_invert_webtoon", false)

internal val ReaderPreferences.dualPageRotateToFit: Preference<Boolean>
    get() = preferenceStore.getBoolean("pref_dual_page_rotate", false)

internal val ReaderPreferences.dualPageRotateToFitInvert: Preference<Boolean>
    get() = preferenceStore.getBoolean(
        "pref_dual_page_rotate_invert",
        false,
    )

internal val ReaderPreferences.dualPageRotateToFitWebtoon: Preference<Boolean>
    get() = preferenceStore.getBoolean(
        "pref_dual_page_rotate_webtoon",
        false,
    )

internal val ReaderPreferences.dualPageRotateToFitInvertWebtoon: Preference<Boolean>
    get() = preferenceStore.getBoolean(
        "pref_dual_page_rotate_invert_webtoon",
        false,
    )

// endregion
