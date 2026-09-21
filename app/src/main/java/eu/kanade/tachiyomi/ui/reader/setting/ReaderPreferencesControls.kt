package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.TappingInvertMode
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.getEnum

// Reader controls: long tap, volume keys and tap-zone navigation.
// region Controls

internal val ReaderPreferences.readWithLongTap: Preference<Boolean>
    get() = preferenceStore.getBoolean("reader_long_tap", true)

internal val ReaderPreferences.readWithVolumeKeys: Preference<Boolean>
    get() = preferenceStore.getBoolean("reader_volume_keys", false)

internal val ReaderPreferences.readWithVolumeKeysInverted: Preference<Boolean>
    get() = preferenceStore.getBoolean(
        "reader_volume_keys_inverted",
        false,
    )

internal val ReaderPreferences.navigationModePager: Preference<Int>
    get() = preferenceStore.getInt("reader_navigation_mode_pager", 0)

internal val ReaderPreferences.navigationModeWebtoon: Preference<Int>
    get() = preferenceStore.getInt("reader_navigation_mode_webtoon", 0)

internal val ReaderPreferences.pagerNavInverted: Preference<TappingInvertMode>
    get() = preferenceStore.getEnum(
        "reader_tapping_inverted",
        TappingInvertMode.NONE,
    )

internal val ReaderPreferences.webtoonNavInverted: Preference<TappingInvertMode>
    get() = preferenceStore.getEnum(
        "reader_tapping_inverted_webtoon",
        TappingInvertMode.NONE,
    )

internal val ReaderPreferences.showNavigationOverlayNewUser: Preference<Boolean>
    get() = preferenceStore.getBoolean(
        "reader_navigation_overlay_new_user",
        true,
    )

internal val ReaderPreferences.showNavigationOverlayOnStart: Preference<Boolean>
    get() = preferenceStore.getBoolean(
        "reader_navigation_overlay_on_start",
        false,
    )

// endregion
