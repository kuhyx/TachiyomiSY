package eu.kanade.presentation.more.settings.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.ResetViewerFlags
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun getLibraryGroup(
    libraryPreferences: LibraryPreferences,
): Preference.PreferenceGroup {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.label_library),
        preferenceItems = listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_refresh_library_covers),
                onClick = { LibraryUpdateJob.startNow(context, target = LibraryUpdateJob.Target.COVERS) },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_reset_viewer_flags),
                subtitle = stringResource(MR.strings.pref_reset_viewer_flags_summary),
                onClick = {
                    scope.launchNonCancellable {
                        val success = Injekt.get<ResetViewerFlags>().await()
                        withUIContext {
                            val message = if (success) {
                                MR.strings.pref_reset_viewer_flags_success
                            } else {
                                MR.strings.pref_reset_viewer_flags_error
                            }
                            context.toast(message)
                        }
                    }
                },
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.updateMangaTitles,
                title = stringResource(MR.strings.pref_update_library_manga_titles),
                subtitle = stringResource(MR.strings.pref_update_library_manga_titles_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.disallowNonAsciiFilenames,
                title = stringResource(MR.strings.pref_disallow_non_ascii_filenames),
                subtitle = stringResource(MR.strings.pref_disallow_non_ascii_filenames_details),
            ),
        ),
    )
}

// SY ->
@Composable
internal fun getDownloadsGroup(
    downloadPreferences: DownloadPreferences,
): Preference.PreferenceGroup {
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.pref_category_downloads),
        preferenceItems = listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.includeChapterUrlHash,
                title = stringResource(SYMR.strings.pref_include_chapter_url_hash),
                subtitle = stringResource(SYMR.strings.pref_include_chapter_url_hash_desc),
            ),
        ),
    )
}

@Composable
internal fun getReaderGroup(
    basePreferences: BasePreferences,
): Preference.PreferenceGroup {
    val context = LocalContext.current
    val chooseColorProfile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            basePreferences.displayProfile.set(uri.toString())
        }
    }
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.pref_category_reader),
        preferenceItems = listOf(
            Preference.PreferenceItem.ListPreference(
                preference = basePreferences.hardwareBitmapThreshold,
                entries = GLUtil.CUSTOM_TEXTURE_LIMIT_OPTIONS
                    .mapIndexed { index, option ->
                        val display = if (index == 0) {
                            stringResource(MR.strings.pref_hardware_bitmap_threshold_default, option)
                        } else {
                            option.toString()
                        }
                        option to display
                    }
                    .toMap(),
                title = stringResource(MR.strings.pref_hardware_bitmap_threshold),
                subtitleProvider = { value, options ->
                    stringResource(MR.strings.pref_hardware_bitmap_threshold_summary, options[value].orEmpty())
                },
                enabled = !ImageUtil.HARDWARE_BITMAP_UNSUPPORTED &&
                    GLUtil.DEVICE_TEXTURE_LIMIT > GLUtil.SAFE_TEXTURE_LIMIT,
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = basePreferences.alwaysDecodeLongStripWithSSIV,
                title = stringResource(MR.strings.pref_always_decode_long_strip_with_ssiv_2),
                subtitle = stringResource(MR.strings.pref_always_decode_long_strip_with_ssiv_summary),
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_display_profile),
                subtitle = basePreferences.displayProfile.get(),
                onClick = {
                    chooseColorProfile.launch(arrayOf("*/*"))
                },
            ),
        ),
    )
}
