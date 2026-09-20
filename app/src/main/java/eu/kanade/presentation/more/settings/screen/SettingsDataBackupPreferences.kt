package eu.kanade.presentation.more.settings.screen

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.data.CreateBackupScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.presentation.more.settings.screen.data.StorageInfo
import eu.kanade.presentation.more.settings.widget.BasePreferenceWidget
import eu.kanade.presentation.more.settings.widget.PrefsHorizontalPadding
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.data.export.LibraryExporter
import eu.kanade.tachiyomi.data.export.LibraryExporter.ExportOptions
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val SIX_HOURS = 6
private const val TWELVE_HOURS = 12
private const val ONE_DAY_HOURS = 24
private const val TWO_DAYS_HOURS = 48
private const val ONE_WEEK_HOURS = 168

/*
 * The backup / restore, data and export groups of the data settings screen.
 * Part of [SettingsDataScreen]; same package, so its getPreferences() calls them as before.
 */

@Composable
internal fun getBackupAndRestoreGroup(backupPreferences: BackupPreferences): Preference.PreferenceGroup {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow

    val lastAutoBackup by backupPreferences.lastAutoBackupTimestamp.collectAsState()

    val chooseBackup = rememberLauncherForActivityResult(
        object : ActivityResultContracts.GetContent() {
            override fun createIntent(context: Context, input: String): Intent {
                val intent = super.createIntent(context, input)
                return Intent.createChooser(intent, context.stringResource(MR.strings.file_select_backup))
            }
        },
    ) {
        if (it == null) {
            context.toast(MR.strings.file_null_uri_error)
        } else {
            navigator.push(RestoreBackupScreen(it.toString()))
        }
    }

    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.label_backup),
        preferenceItems = listOf(
            // Manual actions
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(SettingsDataScreen.restorePreferenceKeyString),
            ) {
                BasePreferenceWidget(
                    subcomponent = {
                        MultiChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(intrinsicSize = IntrinsicSize.Min)
                                .padding(horizontal = PrefsHorizontalPadding),
                        ) {
                            SegmentedButton(
                                modifier = Modifier.fillMaxHeight(),
                                checked = false,
                                onCheckedChange = { navigator.push(CreateBackupScreen()) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) {
                                Text(stringResource(MR.strings.pref_create_backup))
                            }
                            SegmentedButton(
                                modifier = Modifier.fillMaxHeight(),
                                checked = false,
                                onCheckedChange = {
                                    if (!BackupRestoreJob.isRunning(context)) {
                                        if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                                            context.toast(MR.strings.restore_miui_warning)
                                        }

                                        // no need to catch because it's wrapped with a chooser
                                        chooseBackup.launch("*/*")
                                    } else {
                                        context.toast(MR.strings.restore_in_progress)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) {
                                Text(stringResource(MR.strings.pref_restore_backup))
                            }
                        }
                    },
                )
            },

            // Automatic backups
            Preference.PreferenceItem.ListPreference(
                preference = backupPreferences.backupInterval,
                entries = mapOf(
                    0 to stringResource(MR.strings.off),
                    SIX_HOURS to stringResource(MR.strings.update_6hour),
                    TWELVE_HOURS to stringResource(MR.strings.update_12hour),
                    ONE_DAY_HOURS to stringResource(MR.strings.update_24hour),
                    TWO_DAYS_HOURS to stringResource(MR.strings.update_48hour),
                    ONE_WEEK_HOURS to stringResource(MR.strings.update_weekly),
                ),
                title = stringResource(MR.strings.pref_backup_interval),
                onValueChanged = {
                    BackupCreateJob.setupTask(context, it)
                    true
                },
            ),
            Preference.PreferenceItem.InfoPreference(
                stringResource(MR.strings.backup_info) + "\n\n" +
                    stringResource(MR.strings.last_auto_backup_info, relativeTimeSpanString(lastAutoBackup)),
            ),
        ),
    )
}

@Composable
internal fun getDataGroup(): Preference.PreferenceGroup {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_clear_chapter_cache),
                subtitle = stringResource(MR.strings.used_cache, cacheReadableSize),
                onClick = {
                    scope.launchNonCancellable {
                        try {
                            val deletedFiles = chapterCache.clear()
                            withUIContext {
                                context.toast(context.stringResource(MR.strings.cache_deleted, deletedFiles))
                                cacheReadableSizeSema++
                            }
                        } catch (expected: Throwable) {
                            // Logged whatever the cause; the caller carries on.
                            logcat(LogPriority.ERROR, expected)
                            withUIContext { context.toast(MR.strings.cache_delete_error) }
                        }
                    }
                },
            ),
            // SY -->
            Preference.PreferenceItem.TextPreference(
                title = stringResource(SYMR.strings.pref_clear_page_preview_cache),
                subtitle = stringResource(MR.strings.used_cache, pagePreviewReadableSize),
                onClick = {
                    scope.launchNonCancellable {
                        try {
                            val deletedFiles = pagePreviewCache.clear()
                            withUIContext {
                                context.toast(context.stringResource(MR.strings.cache_deleted, deletedFiles))
                                pagePreviewReadableSizeSema++
                            }
                        } catch (expected: Throwable) {
                            // Logged whatever the cause; the caller carries on.
                            logcat(LogPriority.ERROR, expected)
                            withUIContext { context.toast(MR.strings.cache_delete_error) }
                        }
                    }
                },
            ),
            // SY <--
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.autoClearChapterCache,
                title = stringResource(MR.strings.pref_auto_clear_chapter_cache),
            ),
        ),
    )
}

@Composable
internal fun getExportGroup(): Preference.PreferenceGroup {
    var showDialog by remember { mutableStateOf(false) }
    var exportOptions by remember {
        mutableStateOf(
            ExportOptions(
                includeTitle = true,
                includeAuthor = true,
                includeArtist = true,
            ),
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val getFavorites = remember { Injekt.get<GetFavorites>() }
    var favorites by remember { mutableStateOf<List<Manga>>(emptyList()) }
    LaunchedEffect(Unit) {
        favorites = getFavorites.await()
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let {
            scope.launch {
                LibraryExporter.exportToCsv(
                    context = context,
                    uri = it,
                    favorites = favorites,
                    options = exportOptions,
                    onExportComplete = {
                        scope.launch(Dispatchers.Main) {
                            context.toast(MR.strings.library_exported)
                        }
                    },
                )
            }
        }
    }

    if (showDialog) {
        ColumnSelectionDialog(
            options = exportOptions,
            onConfirm = { options ->
                exportOptions = options
                saveFileLauncher.launch("mihon_library.csv")
            },
            onDismissRequest = { showDialog = false },
        )
    }

    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.export),
        preferenceItems = listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.library_list),
                onClick = { showDialog = true },
            ),
        ),
    )
}

@Composable
internal fun ColumnSelectionDialog(
    options: ExportOptions,
    onConfirm: (ExportOptions) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var titleSelected by remember { mutableStateOf(options.includeTitle) }
    var authorSelected by remember { mutableStateOf(options.includeAuthor) }
    var artistSelected by remember { mutableStateOf(options.includeArtist) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.migration_dialog_what_to_include))
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = titleSelected,
                        onCheckedChange = { checked ->
                            titleSelected = checked
                            if (!checked) {
                                authorSelected = false
                                artistSelected = false
                            }
                        },
                    )
                    Text(text = stringResource(MR.strings.title))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = authorSelected,
                        onCheckedChange = { authorSelected = it },
                        enabled = titleSelected,
                    )
                    Text(text = stringResource(MR.strings.author))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = artistSelected,
                        onCheckedChange = { artistSelected = it },
                        enabled = titleSelected,
                    )
                    Text(text = stringResource(MR.strings.artist))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ExportOptions(
                            includeTitle = titleSelected,
                            includeAuthor = authorSelected,
                            includeArtist = artistSelected,
                        ),
                    )
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
