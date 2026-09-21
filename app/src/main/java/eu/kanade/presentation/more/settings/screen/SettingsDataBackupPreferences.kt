package eu.kanade.presentation.more.settings.screen

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.data.CreateBackupScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.presentation.more.settings.widget.BasePreferenceWidget
import eu.kanade.presentation.more.settings.widget.PrefsHorizontalPadding
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
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

// Picks the backup file through the system chooser and opens the restore screen for it.
@Composable
private fun rememberRestoreAction(): () -> Unit {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
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
    return {
        if (!BackupRestoreJob.isRunning(context)) {
            if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                context.toast(MR.strings.restore_miui_warning)
            }
            // no need to catch because it's wrapped with a chooser
            chooseBackup.launch("*/*")
        } else {
            context.toast(MR.strings.restore_in_progress)
        }
    }
}

@Composable
private fun CreateRestoreButtons(onRestoreClick: () -> Unit) {
    val navigator = LocalNavigator.currentOrThrow
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
            onCheckedChange = { onRestoreClick() },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
        ) {
            Text(stringResource(MR.strings.pref_restore_backup))
        }
    }
}

@Composable
internal fun getBackupAndRestoreGroup(backupPreferences: BackupPreferences): Preference.PreferenceGroup {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow

    val lastAutoBackup by backupPreferences.lastAutoBackupTimestamp.collectAsState()

    val onRestoreClick = rememberRestoreAction()

    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.label_backup),
        preferenceItems = listOf(
            // Manual actions
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(SettingsDataScreen.restorePreferenceKeyString),
            ) {
                BasePreferenceWidget(
                    subcomponent = { CreateRestoreButtons(onRestoreClick) },
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
