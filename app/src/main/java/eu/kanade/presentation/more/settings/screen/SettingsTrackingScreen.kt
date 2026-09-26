package eu.kanade.presentation.more.settings.screen

import android.net.Uri
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
import eu.kanade.tachiyomi.data.track.bangumi.BangumiApi
import eu.kanade.tachiyomi.data.track.hikka.HikkaApi
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBakaApi
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi
import eu.kanade.tachiyomi.data.track.shikimori.ShikimoriApi
import eu.kanade.tachiyomi.util.system.openInBrowser
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal object SettingsTrackingScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_tracking

    @Composable
    override fun RowScope.AppBarAction() {
        val uriHandler = LocalUriHandler.current
        IconButton(onClick = { uriHandler.openUri("https://mihon.app/docs/guides/tracking") }) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = stringResource(MR.strings.tracking_guide),
            )
        }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val trackPreferences = remember { Injekt.get<TrackPreferences>() }
        val trackerManager = remember { Injekt.get<TrackerManager>() }
        val sourceManager = remember { Injekt.get<SourceManager>() }

        var dialog by remember { mutableStateOf<Any?>(null) }
        dialog?.run {
            // Logout is the only other dialog: an `is` check for it would leave a dead "no match" group.
            if (this is LoginDialog) {
                TrackingLoginDialog(
                    tracker = tracker,
                    uNameStringRes = uNameStringRes,
                    onDismissRequest = { dialog = null },
                )
            } else {
                TrackingLogoutDialog(
                    tracker = (this as LogoutDialog).tracker,
                    onDismissRequest = { dialog = null },
                )
            }
        }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = trackPreferences.autoUpdateTrack,
                title = stringResource(MR.strings.pref_auto_update_manga_sync),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = trackPreferences.autoUpdateTrackOnMarkRead,
                entries = AutoTrackState.entries
                    .associateWith { stringResource(it.titleRes) },
                title = stringResource(MR.strings.pref_auto_update_manga_on_mark_read),
            ),
            // SY -->
            Preference.PreferenceItem.SwitchPreference(
                preference = trackPreferences.resolveUsingSourceMetadata,
                title = stringResource(SYMR.strings.pref_tracker_resolve_using_source_metadata),
                subtitle = stringResource(SYMR.strings.pref_tracker_resolve_using_source_metadata_summary),
            ),
            // SY <--
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.services),
                preferenceItems = servicePreferences(trackerManager) { dialog = it },
            ),
            enhancedServicesGroup(trackerManager, sourceManager),
        )
    }

    // OAuth trackers log in through the browser; the credential-based ones open the login dialog.
    @Composable
    private fun servicePreferences(
        trackerManager: TrackerManager,
        openDialog: (Any) -> Unit,
    ): List<Preference.PreferenceItem<out Any, out Any>> {
        val context = LocalContext.current
        fun browserLogin(tracker: Tracker, authUrl: Uri) = Preference.PreferenceItem.TrackerPreference(
            tracker = tracker,
            login = { context.openInBrowser(authUrl, forceDefaultBrowser = true) },
            logout = { openDialog(LogoutDialog(tracker)) },
        )
        fun credentialLogin(tracker: Tracker, uNameStringRes: StringResource) =
            Preference.PreferenceItem.TrackerPreference(
                tracker = tracker,
                login = { openDialog(LoginDialog(tracker, uNameStringRes)) },
                logout = { openDialog(LogoutDialog(tracker)) },
            )
        return listOf(
            browserLogin(trackerManager.mangaBaka, MangaBakaApi.authUrl()),
            browserLogin(trackerManager.myAnimeList, MyAnimeListApi.authUrl()),
            browserLogin(trackerManager.aniList, AnilistApi.authUrl()),
            credentialLogin(trackerManager.kitsu, MR.strings.email),
            credentialLogin(trackerManager.mangaUpdates, MR.strings.username),
            browserLogin(trackerManager.shikimori, ShikimoriApi.authUrl()),
            browserLogin(trackerManager.bangumi, BangumiApi.authUrl()),
            browserLogin(trackerManager.hikka, HikkaApi.authUrl()),
            Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.tracking_info)),
        )
    }

    // Enhanced trackers that have a matching source installed are listed; the rest are named in the info text.
    @Composable
    private fun enhancedServicesGroup(
        trackerManager: TrackerManager,
        sourceManager: SourceManager,
    ): Preference.PreferenceGroup {
        val enhancedTrackers = trackerManager.trackers
            .filter { it is EnhancedTracker }
            .partition { service ->
                val acceptedSources = (service as EnhancedTracker).getAcceptedSources()
                sourceManager.getAll().any { it::class.qualifiedName in acceptedSources }
            }
        var enhancedTrackerInfo = stringResource(MR.strings.enhanced_tracking_info)
        if (enhancedTrackers.second.isNotEmpty()) {
            val missingSourcesInfo = stringResource(
                MR.strings.enhanced_services_not_installed,
                enhancedTrackers.second.joinToString { it.name },
            )
            enhancedTrackerInfo += "\n\n$missingSourcesInfo"
        }
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.enhanced_services),
            preferenceItems = enhancedTrackers.first
                .map { service ->
                    Preference.PreferenceItem.TrackerPreference(
                        tracker = service,
                        login = { (service as EnhancedTracker).loginNoop() },
                        logout = service::logout,
                    )
                } + listOf(Preference.PreferenceItem.InfoPreference(enhancedTrackerInfo)),
        )
    }
}
