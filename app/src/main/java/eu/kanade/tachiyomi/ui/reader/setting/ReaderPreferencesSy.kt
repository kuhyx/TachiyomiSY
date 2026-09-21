package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.ArchiveReaderMode
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import tachiyomi.core.common.preference.Preference

private const val DEFAULT_AUTOSCROLL_SECONDS = 3f
private const val DEFAULT_PRELOAD_PAGES = 10

// The fork's own reader preferences.
// SY -->

internal val ReaderPreferences.readerThreads: Preference<Int>
    get() = preferenceStore.getInt("eh_reader_threads", 2)

internal val ReaderPreferences.readerInstantRetry: Preference<Boolean>
    get() = preferenceStore.getBoolean("eh_reader_instant_retry", true)

internal val ReaderPreferences.aggressivePageLoading: Preference<Boolean>
    get() = preferenceStore.getBoolean("eh_aggressive_page_loading", false)

internal val ReaderPreferences.cacheSize: Preference<String>
    get() = preferenceStore.getString("eh_cache_size", "75")

internal val ReaderPreferences.autoscrollInterval: Preference<Float>
    get() = preferenceStore.getFloat("eh_util_autoscroll_interval", DEFAULT_AUTOSCROLL_SECONDS)

internal val ReaderPreferences.smoothAutoScroll: Preference<Boolean>
    get() = preferenceStore.getBoolean("smooth_auto_scroll", true)

internal val ReaderPreferences.preserveReadingPosition: Preference<Boolean>
    get() = preferenceStore.getBoolean("eh_preserve_reading_position", false)

internal val ReaderPreferences.preloadSize: Preference<Int>
    get() = preferenceStore.getInt("eh_preload_size", DEFAULT_PRELOAD_PAGES)

internal val ReaderPreferences.useAutoWebtoon: Preference<Boolean>
    get() = preferenceStore.getBoolean("eh_use_auto_webtoon", true)

internal val ReaderPreferences.continuousVerticalTappingByPage: Preference<Boolean>
    get() = preferenceStore.getBoolean("continuous_vertical_tapping_by_page", false)

internal val ReaderPreferences.cropBordersContinuousVertical: Preference<Boolean>
    get() = preferenceStore.getBoolean("crop_borders_continues_vertical", false)

internal val ReaderPreferences.readerBottomButtons: Preference<Set<String>>
    get() = preferenceStore.getStringSet("reader_bottom_buttons", ReaderBottomButton.BUTTONS_DEFAULTS)

internal val ReaderPreferences.pageLayout: Preference<Int>
    get() = preferenceStore.getInt("page_layout", PagerConfig.PageLayout.AUTOMATIC)

internal val ReaderPreferences.invertDoublePages: Preference<Boolean>
    get() = preferenceStore.getBoolean("invert_double_pages", false)

internal val ReaderPreferences.centerMarginType: Preference<Int>
    get() = preferenceStore.getInt("center_margin_type", PagerConfig.CenterMarginType.NONE)

internal val ReaderPreferences.archiveReaderMode: Preference<Int>
    get() = preferenceStore.getInt("archive_reader_mode", ArchiveReaderMode.LOAD_FROM_FILE)
// SY <--
