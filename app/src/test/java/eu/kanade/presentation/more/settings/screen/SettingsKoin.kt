package eu.kanade.presentation.more.settings.screen

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import exh.source.ExhPreferences
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.storage.FolderProvider
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.storage.service.StoragePreferences

/**
 * Every preference class the settings screens read, real and backed by one [MapPreferenceStore], so a
 * value set through a screen's callback is read back by the next composition. Screens that also pull
 * interactors or managers pass them as [start]'s extra modules.
 */
internal class SettingsKoin {
    val store: MapPreferenceStore = MapPreferenceStore()
    val context: Application = ApplicationProvider.getApplicationContext()
    val ui: UiPreferences = UiPreferences(store)
    val source: SourcePreferences = SourcePreferences(store)
    val library: LibraryPreferences = LibraryPreferences(store)
    val exh: ExhPreferences = ExhPreferences(store)
    val security: SecurityPreferences = SecurityPreferences(store)
    val download: DownloadPreferences = DownloadPreferences(store)
    val base: BasePreferences = BasePreferences(context, store)
    val sync: SyncPreferences = SyncPreferences(store)
    val privacy: PrivacyPreferences = PrivacyPreferences(store)
    val track: TrackPreferences = TrackPreferences(store)
    val reader: ReaderPreferences = ReaderPreferences(store)
    val network: NetworkPreferences = NetworkPreferences(store)
    val backup: BackupPreferences = BackupPreferences(store)
    val folders: FolderProvider = mockk(relaxed = true)
    val storage: StoragePreferences = StoragePreferences(folders, store)

    fun start(vararg extra: Module) {
        stopKoin()
        startKoin {
            allowOverride(true)
            modules(
                module {
                    single<PreferenceStore> { store }
                    single<Application> { context }
                    single { ui }
                    single { source }
                    single { library }
                    single { exh }
                    single { security }
                    single { download }
                    single { base }
                    single { sync }
                    single { privacy }
                    single { track }
                    single { reader }
                    single { network }
                    single { backup }
                    single { storage }
                },
                *extra,
            )
        }
    }

    fun stop() {
        stopKoin()
    }
}
