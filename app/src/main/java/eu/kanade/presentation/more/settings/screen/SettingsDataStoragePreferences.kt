package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.data.StorageInfo
import eu.kanade.presentation.more.settings.widget.BasePreferenceWidget
import eu.kanade.presentation.more.settings.widget.PrefsHorizontalPadding
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// A "clear this cache" row; [clear] returns how many files went, [onCleared] refreshes the shown size.
@Composable
internal fun clearCachePreference(
    title: String,
    readableSize: String,
    clear: () -> Int,
    onCleared: () -> Unit,
): Preference.PreferenceItem<out Any, out Any> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return Preference.PreferenceItem.TextPreference(
        title = title,
        subtitle = stringResource(MR.strings.used_cache, readableSize),
        onClick = {
            scope.launchNonCancellable {
                try {
                    val deletedFiles = clear()
                    withUIContext {
                        context.toast(context.stringResource(MR.strings.cache_deleted, deletedFiles))
                        onCleared()
                    }
                } catch (expected: Throwable) {
                    // Logged whatever the cause; the caller carries on.
                    logcat(LogPriority.ERROR, expected)
                    withUIContext { context.toast(MR.strings.cache_delete_error) }
                }
            }
        },
    )
}

@Composable
internal fun getDataGroup(): Preference.PreferenceGroup {
    val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }

    val chapterCache = remember { Injekt.get<ChapterCache>() }
    var cacheReadableSizeSema by remember { mutableIntStateOf(0) }
    val cacheReadableSize = remember(cacheReadableSizeSema) { chapterCache.readableSize }

    // SY -->
    val pagePreviewCache = remember { Injekt.get<PagePreviewCache>() }
    var pagePreviewReadableSizeSema by remember { mutableIntStateOf(0) }
    val pagePreviewReadableSize = remember(pagePreviewReadableSizeSema) { pagePreviewCache.readableSize }
    // SY <--

    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.pref_storage_usage),
        preferenceItems = listOf(
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(MR.strings.pref_storage_usage),
            ) {
                BasePreferenceWidget(
                    subcomponent = {
                        StorageInfo(
                            modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                        )
                    },
                )
            },

            clearCachePreference(
                title = stringResource(MR.strings.pref_clear_chapter_cache),
                readableSize = cacheReadableSize,
                clear = chapterCache::clear,
                onCleared = { cacheReadableSizeSema++ },
            ),
            // SY -->
            clearCachePreference(
                title = stringResource(SYMR.strings.pref_clear_page_preview_cache),
                readableSize = pagePreviewReadableSize,
                clear = pagePreviewCache::clear,
                onCleared = { pagePreviewReadableSizeSema++ },
            ),
            // SY <--
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.autoClearChapterCache,
                title = stringResource(MR.strings.pref_auto_clear_chapter_cache),
            ),
        ),
    )
}
