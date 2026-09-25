package eu.kanade.domain.source.service

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.tachiyomi.util.system.LocaleHelper
import io.kotest.matchers.shouldBe
import mihon.domain.migration.models.MigrationFlag
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.library.model.LibraryDisplayMode

internal class SourcePreferencesTest {

    private val preferences = SourcePreferences(InMemoryPreferenceStore())

    @Test
    fun browseDefaults() {
        preferences.sourceDisplayMode.get() shouldBe LibraryDisplayMode.default
        preferences.sourceDisplayMode.key() shouldBe "pref_display_mode_catalogue"
        preferences.enabledLanguages.get() shouldBe LocaleHelper.getDefaultEnabledLanguages()
        preferences.disabledSources.get() shouldBe emptySet()
        preferences.incognitoExtensions.get() shouldBe emptySet()
        preferences.pinnedSources.get() shouldBe emptySet()
        preferences.lastUsedSource.get() shouldBe -1L
        Preference.isAppState(preferences.lastUsedSource.key()) shouldBe true
        preferences.showNsfwSource.get() shouldBe true
        preferences.hideInLibraryItems.get() shouldBe false
        preferences.extensionRepos.get() shouldBe emptySet()
        preferences.extensionUpdatesCount.get() shouldBe 0
        preferences.trustedExtensions.get() shouldBe emptySet()
        Preference.isAppState(preferences.trustedExtensions.key()) shouldBe true
        preferences.globalSearchFilterState.get() shouldBe false
    }

    @Test
    fun migrationDefaults() {
        preferences.migrationSortingMode.get() shouldBe SetMigrateSorting.Mode.ALPHABETICAL
        preferences.migrationSortingDirection.get() shouldBe SetMigrateSorting.Direction.ASCENDING
        preferences.migrationSources.get() shouldBe emptyList()
        preferences.migrationFlags.get() shouldBe MigrationFlag.entries.toSet()
        preferences.migrationDeepSearchMode.get() shouldBe false
        preferences.migrationPrioritizeByChapters.get() shouldBe false
        preferences.migrationHideUnmatched.get() shouldBe false
        preferences.migrationHideWithoutUpdates.get() shouldBe false
    }

    @Test
    fun syDefaults() {
        preferences.enableSourceBlacklist.get() shouldBe true
        preferences.sourcesTabCategories.get() shouldBe emptySet()
        preferences.sourcesTabCategoriesFilter.get() shouldBe false
        preferences.sourcesTabSourcesInCategories.get() shouldBe emptySet()
        preferences.dataSaver.get() shouldBe SourcePreferences.DataSaver.NONE
        preferences.dataSaverIgnoreJpeg.get() shouldBe false
        preferences.dataSaverIgnoreGif.get() shouldBe true
        preferences.dataSaverImageQuality.get() shouldBe 80
        preferences.dataSaverImageFormatJpeg.get() shouldBe false
        preferences.dataSaverServer.get() shouldBe ""
        preferences.dataSaverColorBW.get() shouldBe false
        preferences.dataSaverExcludedSources.get() shouldBe emptySet()
        preferences.dataSaverDownloader.get() shouldBe true
        preferences.allowLocalSourceHiddenFolders.get() shouldBe false
        preferences.preferredMangaDexId.get() shouldBe "0"
        preferences.mangadexSyncToLibraryIndexes.get() shouldBe emptySet()
        preferences.recommendationSearchFlags.get() shouldBe Int.MAX_VALUE
        SourcePreferences.DataSaver.entries.size shouldBe 3
        SourcePreferences.DataSaver.valueOf("WSRV_NL") shouldBe SourcePreferences.DataSaver.WSRV_NL
    }

    @Test
    fun migrationFlagsRoundTripAsBits() {
        val store = FlowPreferenceStore()
        val flowPreferences = SourcePreferences(store)
        flowPreferences.migrationFlags.set(setOf(MigrationFlag.CHAPTER))
        store.getInt("migration_flags").get() shouldBe MigrationFlag.toBit(setOf(MigrationFlag.CHAPTER))
        flowPreferences.migrationFlags.get() shouldBe setOf(MigrationFlag.CHAPTER)
        flowPreferences.migrationSources.set(listOf(3L, 4L))
        flowPreferences.migrationSources.get() shouldBe listOf(3L, 4L)
    }
}
