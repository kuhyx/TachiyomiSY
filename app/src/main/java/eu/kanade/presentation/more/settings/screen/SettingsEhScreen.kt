package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.presentation.more.settings.Preference
import exh.source.ExhPreferences
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.interactor.DeleteFavoriteEntries
import tachiyomi.domain.manga.interactor.GetExhFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Batch-interval choices in hours, and the calendar units the relative-time formatter counts in.

internal object SettingsEhScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = SYMR.strings.pref_category_eh

    override fun isEnabled(): Boolean = Injekt.get<ExhPreferences>().isHentaiEnabled.get()

    @Composable
    fun Reconfigure(
        exhPreferences: ExhPreferences,
        openWarnConfigureDialogController: () -> Unit,
    ) {
        var initialLoadGuard by remember { mutableStateOf(false) }
        val useHentaiAtHome by exhPreferences.useHentaiAtHome.collectAsState()
        val useJapaneseTitle by exhPreferences.useJapaneseTitle.collectAsState()
        val useOriginalImages by exhPreferences.exhUseOriginalImages.collectAsState()
        val ehTagFilterValue by exhPreferences.ehTagFilterValue.collectAsState()
        val ehTagWatchingValue by exhPreferences.ehTagWatchingValue.collectAsState()
        val settingsLanguages by exhPreferences.exhSettingsLanguages.collectAsState()
        val enabledCategories by exhPreferences.exhEnabledCategories.collectAsState()
        val imageQuality by exhPreferences.imageQuality.collectAsState()
        DisposableEffect(
            useHentaiAtHome,
            useJapaneseTitle,
            useOriginalImages,
            ehTagFilterValue,
            ehTagWatchingValue,
            settingsLanguages,
            enabledCategories,
            imageQuality,
        ) {
            if (initialLoadGuard) {
                openWarnConfigureDialogController()
            }
            initialLoadGuard = true
            onDispose {}
        }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val exhPreferences: ExhPreferences = remember { Injekt.get() }
        val getFlatMetadataById: GetFlatMetadataById = remember { Injekt.get() }
        val deleteFavoriteEntries: DeleteFavoriteEntries = remember { Injekt.get() }
        val getExhFavoriteMangaWithMetadata: GetExhFavoriteMangaWithMetadata = remember { Injekt.get() }
        val exhentaiEnabled by exhPreferences.enableExhentai.collectAsState()
        var runConfigureDialog by remember { mutableStateOf(false) }
        val openWarnConfigureDialogController = { runConfigureDialog = true }

        Reconfigure(exhPreferences, openWarnConfigureDialogController)

        ConfigureExhDialog(run = runConfigureDialog, onRunning = { runConfigureDialog = false })

        return listOf(
            Preference.PreferenceGroup(
                stringResource(SYMR.strings.ehentai_prefs_account_settings),
                preferenceItems = listOf(
                    getLoginPreference(exhPreferences, openWarnConfigureDialogController),
                    useHentaiAtHome(exhentaiEnabled, exhPreferences),
                    useJapaneseTitle(exhentaiEnabled, exhPreferences),
                    useOriginalImages(exhentaiEnabled, exhPreferences),
                    watchedTags(exhentaiEnabled),
                    tagFilterThreshold(exhentaiEnabled, exhPreferences),
                    tagWatchingThreshold(exhentaiEnabled, exhPreferences),
                    settingsLanguages(exhentaiEnabled, exhPreferences),
                    enabledCategories(exhentaiEnabled, exhPreferences),
                    watchedListDefaultState(exhentaiEnabled, exhPreferences),
                    imageQuality(exhentaiEnabled, exhPreferences),
                    enhancedEhentaiView(exhPreferences),
                ),
            ),
            Preference.PreferenceGroup(
                stringResource(SYMR.strings.favorites_sync),
                preferenceItems = listOf(
                    readOnlySync(exhPreferences),
                    syncFavoriteNotes(),
                    lenientSync(exhPreferences),
                    forceSyncReset(deleteFavoriteEntries),
                ),
            ),
            Preference.PreferenceGroup(
                stringResource(SYMR.strings.gallery_update_checker),
                preferenceItems = listOf(
                    updateCheckerFrequency(exhPreferences),
                    autoUpdateRequirements(exhPreferences),
                    updaterStatistics(
                        exhPreferences,
                        getExhFavoriteMangaWithMetadata,
                        getFlatMetadataById,
                    ),
                ),
            ),
        )
    }
}
