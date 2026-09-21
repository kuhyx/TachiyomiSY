package eu.kanade.presentation.more.settings.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import exh.assets.EhAssets
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

internal data class SettingsMainItem(
    val titleRes: StringResource,
    val subtitleRes: StringResource,
    val formatSubtitle: @Composable () -> String = { stringResource(subtitleRes) },
    val icon: ImageVector,
    val screen: VoyagerScreen,
)

internal val settingsMainItems = listOf(
    SettingsMainItem(
        titleRes = MR.strings.pref_category_appearance,
        subtitleRes = MR.strings.pref_appearance_summary,
        icon = Icons.Outlined.Palette,
        screen = SettingsAppearanceScreen,
    ),
    SettingsMainItem(
        titleRes = MR.strings.pref_category_library,
        subtitleRes = MR.strings.pref_library_summary,
        icon = Icons.Outlined.CollectionsBookmark,
        screen = SettingsLibraryScreen,
    ),
    SettingsMainItem(
        titleRes = MR.strings.pref_category_reader,
        subtitleRes = MR.strings.pref_reader_summary,
        icon = Icons.AutoMirrored.Outlined.ChromeReaderMode,
        screen = SettingsReaderScreen,
    ),
    SettingsMainItem(
        titleRes = MR.strings.pref_category_downloads,
        subtitleRes = MR.strings.pref_downloads_summary,
        icon = Icons.Outlined.GetApp,
        screen = SettingsDownloadScreen,
    ),
    SettingsMainItem(
        titleRes = MR.strings.pref_category_tracking,
        subtitleRes = MR.strings.pref_tracking_summary,
        icon = Icons.Outlined.Sync,
        screen = SettingsTrackingScreen,
    ),
    SettingsMainItem(
        titleRes = MR.strings.browse,
        subtitleRes = MR.strings.pref_browse_summary,
        icon = Icons.Outlined.Explore,
        screen = SettingsBrowseScreen,
    ),
    SettingsMainItem(
        titleRes = MR.strings.label_data_storage,
        subtitleRes = MR.strings.pref_backup_summary,
        icon = Icons.Outlined.Storage,
        screen = SettingsDataScreen,
    ),
    SettingsMainItem(
        titleRes = MR.strings.pref_category_security,
        subtitleRes = MR.strings.pref_security_summary,
        icon = Icons.Outlined.Security,
        screen = SettingsSecurityScreen,
    ),
    // SY -->
    SettingsMainItem(
        titleRes = SYMR.strings.pref_category_eh,
        subtitleRes = SYMR.strings.pref_ehentai_summary,
        icon = EhAssets.EhLogo,
        screen = SettingsEhScreen,
    ),
    SettingsMainItem(
        titleRes = SYMR.strings.pref_category_mangadex,
        subtitleRes = SYMR.strings.pref_mangadex_summary,
        icon = EhAssets.MangadexLogo,
        screen = SettingsMangadexScreen,
    ),
    // SY <--
    SettingsMainItem(
        titleRes = MR.strings.pref_category_advanced,
        subtitleRes = MR.strings.pref_advanced_summary,
        icon = Icons.Outlined.Code,
        screen = SettingsAdvancedScreen,
    ),
    SettingsMainItem(
        titleRes = MR.strings.pref_category_about,
        subtitleRes = StringResource(0),
        formatSubtitle = {
            "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}"
        },
        icon = Icons.Outlined.Info,
        screen = AboutScreen,
    ),
)
