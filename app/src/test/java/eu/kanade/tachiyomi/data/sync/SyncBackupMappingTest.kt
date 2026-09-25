package eu.kanade.tachiyomi.data.sync

import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.sync.models.SyncSettings
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.create.creators.CategoriesBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.ExtensionStoresBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.MangaBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.PreferenceBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.SavedSearchBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.SourcesBackupCreator
import eu.kanade.tachiyomi.data.backup.libraryManga
import eu.kanade.tachiyomi.data.backup.chapterRow
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.backupExtensionStore
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import eu.kanade.tachiyomi.data.sync.service.category
import eu.kanade.tachiyomi.data.sync.service.chapter
import eu.kanade.tachiyomi.data.sync.service.manga
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SyncBackupMappingTest {

    private var logged = mutableListOf<String>()

    private val full = Backup(
        backupManga = listOf(manga("a")),
        backupCategories = listOf(category("C")),
        backupSources = listOf(BackupSource(name = "S", sourceId = 1L)),
        backupPreferences = listOf(BackupPreference("k", IntPreferenceValue(1))),
        backupSourcePreferences = listOf(BackupSourcePreferences("s", emptyList())),
        backupExtensionStores = listOf(backupExtensionStore()),
        backupSavedSearches = listOf(BackupSavedSearch(name = "q")),
    )

    @BeforeEach
    fun setUp() {
        logged = captureLogcat()
    }

    @AfterEach
    fun tearDown() = releaseLogcat()

    @Test
    fun settingsBecomeBackupOptions() {
        SyncSettings().toBackupOptions() shouldBe BackupOptions()
        val flipped = SyncSettings(
            libraryEntries = false,
            categories = false,
            chapters = false,
            tracking = false,
            history = false,
            appSettings = false,
            extensionStores = false,
            sourceSettings = false,
            privateSettings = true,
            customInfo = false,
            readEntries = false,
            savedSearches = false,
        )
        flipped.toBackupOptions() shouldBe BackupOptions(
            libraryEntries = false,
            categories = false,
            chapters = false,
            tracking = false,
            history = false,
            readEntries = false,
            appSettings = false,
            extensionStores = false,
            sourceSettings = false,
            privateSettings = true,
            customInfo = false,
            savedSearches = false,
        )
        flipped.toRestoreOptions() shouldBe RestoreOptions(
            libraryEntries = false,
            categories = false,
            appSettings = false,
            extensionStores = false,
            sourceSettings = false,
            savedSearches = false,
        )
        SyncSettings().toRestoreOptions() shouldBe RestoreOptions()
    }

    @Test
    fun emptyRemoteNeedsNoLibrary() {
        Backup(backupManga = emptyList()).isEmptyRemote() shouldBe true
        full.isEmptyRemote() shouldBe false
        full.copy(backupManga = emptyList()).isEmptyRemote() shouldBe false
        full.copy(backupManga = emptyList(), backupCategories = emptyList()).isEmptyRemote() shouldBe false
        Backup(backupManga = emptyList(), backupPreferences = full.backupPreferences).isEmptyRemote() shouldBe true
    }

    @Test
    fun mergeTakesRemoteSections() {
        val favorites = listOf(manga("fav"))
        val local = Backup(backupManga = listOf(manga("local")))
        local.mergedWithRemote(full, favorites) shouldBe full.copy(backupManga = favorites)
    }

    @Test
    fun changesAreFoundPerSection() {
        full.hasChangesFrom(full, emptyList()) shouldBe false
        full.hasChangesFrom(full, listOf(manga("x"))) shouldBe true
        val empty = Backup(backupManga = full.backupManga)
        listOf(
            empty.copy(backupCategories = full.backupCategories),
            empty.copy(backupSources = full.backupSources),
            empty.copy(backupPreferences = full.backupPreferences),
            empty.copy(backupSourcePreferences = full.backupSourcePreferences),
            empty.copy(backupExtensionStores = full.backupExtensionStores),
            empty.copy(backupSavedSearches = full.backupSavedSearches),
        ).forEach { remote -> empty.hasChangesFrom(remote, emptyList()) shouldBe true }
    }

    @Test
    fun chapterListsCompareByUrlAndVersion() {
        val local = listOf(chapterRow(url = "a", version = 1), chapterRow(url = "b", version = 2))
        areChaptersDifferent(local, listOf(chapter("a", version = 1))) shouldBe true
        areChaptersDifferent(local, listOf(chapter("a", version = 1), chapter("c", version = 2))) shouldBe true
        areChaptersDifferent(local, listOf(chapter("a", version = 1), chapter("b", version = 3))) shouldBe true
        areChaptersDifferent(local, listOf(chapter("b", version = 2), chapter("a", version = 1))) shouldBe false
    }

    @Test
    fun syncBackupHasEverySection() = runTest {
        val options = BackupOptions()
        val mangas = listOf(libraryManga(id = 1L))
        val categories = mockk<CategoriesBackupCreator>()
        val library = mockk<MangaBackupCreator>()
        val preferences = mockk<PreferenceBackupCreator>()
        val stores = mockk<ExtensionStoresBackupCreator>()
        val sources = mockk<SourcesBackupCreator>()
        val searches = mockk<SavedSearchBackupCreator>()
        coEvery { categories() } returns full.backupCategories
        coEvery { library(mangas, options) } returns full.backupManga
        every { preferences.createApp(includePrivatePreferences = false) } returns full.backupPreferences
        every { preferences.createSource(includePrivatePreferences = false) } returns full.backupSourcePreferences
        coEvery { stores() } returns full.backupExtensionStores
        every { sources(full.backupManga) } returns full.backupSources
        coEvery { searches() } returns full.backupSavedSearches
        val creator = BackupCreator(
            context = mockk(),
            isAutoBackup = false,
            parser = mockk(),
            getFavorites = mockk(),
            backupPreferences = mockk(),
            mangaRepository = mockk(),
            categoriesBackupCreator = categories,
            mangaBackupCreator = library,
            preferenceBackupCreator = preferences,
            extensionStoresBackupCreator = stores,
            sourcesBackupCreator = sources,
            savedSearchBackupCreator = searches,
            getMergedManga = mockk(),
        )
        creator.createSyncBackup(mangas, options) shouldBe full
        logged shouldContain "Begin create backup"
        logged shouldContain "End create backup"
    }
}
