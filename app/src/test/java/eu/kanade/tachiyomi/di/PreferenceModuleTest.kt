package eu.kanade.tachiyomi.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import exh.pref.DelegateSourcePreferences
import exh.source.ExhPreferences
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.storage.AndroidStorageFolderProvider
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.domain.updates.service.UpdatesPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@RunWith(RobolectricTestRunner::class)
internal class PreferenceModuleTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun startWithFolderProvider(vararg modules: InjektModule) {
        modules.forEach { Injekt.importModule(it) }
        val folders = module { single { AndroidStorageFolderProvider(app) } }
        startKoin { modules(modules.map { it.koinModule() } + folders) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun everyPreferenceResolves() {
        startWithFolderProvider(PreferenceModule(app))
        Injekt.get<PreferenceStore>().shouldBeInstanceOf<AndroidPreferenceStore>()
        Injekt.get<NetworkPreferences>().verboseLogging.get() shouldBe true
        Injekt.get<SourcePreferences>().shouldBeInstanceOf<SourcePreferences>()
        Injekt.get<SecurityPreferences>().shouldBeInstanceOf<SecurityPreferences>()
        Injekt.get<PrivacyPreferences>().shouldBeInstanceOf<PrivacyPreferences>()
        Injekt.get<LibraryPreferences>().shouldBeInstanceOf<LibraryPreferences>()
        Injekt.get<UpdatesPreferences>().shouldBeInstanceOf<UpdatesPreferences>()
        Injekt.get<ReaderPreferences>().shouldBeInstanceOf<ReaderPreferences>()
        Injekt.get<TrackPreferences>().shouldBeInstanceOf<TrackPreferences>()
        Injekt.get<DownloadPreferences>().shouldBeInstanceOf<DownloadPreferences>()
        Injekt.get<BackupPreferences>().shouldBeInstanceOf<BackupPreferences>()
        Injekt.get<StoragePreferences>().shouldBeInstanceOf<StoragePreferences>()
        Injekt.get<UiPreferences>().shouldBeInstanceOf<UiPreferences>()
        Injekt.get<BasePreferences>().context shouldBe app
        Injekt.get<SyncPreferences>().shouldBeInstanceOf<SyncPreferences>()
    }

    @Test
    fun syPreferencesResolve() {
        val sy = SYPreferenceModule(app)
        sy.application shouldBe app
        startWithFolderProvider(PreferenceModule(app), sy)
        Injekt.get<DelegateSourcePreferences>().shouldBeInstanceOf<DelegateSourcePreferences>()
        Injekt.get<ExhPreferences>().shouldBeInstanceOf<ExhPreferences>()
    }
}
