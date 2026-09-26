package eu.kanade.tachiyomi.ui.browse

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.ui.manga.NoCustomInfo
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import exh.source.ExhPreferences
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo

/**
 * The real in-memory preferences every browse screen reads, plus a main dispatcher, around the
 * test's own [Module]s. [stop] also drops the coroutine scopes Voyager cached for the models.
 */
internal class BrowseKoin {
    val app: Application = ApplicationProvider.getApplicationContext()
    val store: FlowPreferenceStore = FlowPreferenceStore()
    val sourcePreferences: SourcePreferences = SourcePreferences(store)
    val basePreferences: BasePreferences = BasePreferences(app, store)
    val libraryPreferences: LibraryPreferences = LibraryPreferences(store)
    val uiPreferences: UiPreferences = UiPreferences(store)
    val exhPreferences: ExhPreferences = ExhPreferences(store)

    fun start(vararg extra: Module) {
        stopKoin()
        startKoin {
            allowOverride(true)
            modules(
                module {
                    single { app }
                    single<PreferenceStore> { store }
                    single { sourcePreferences }
                    single { basePreferences }
                    single { libraryPreferences }
                    single { uiPreferences }
                    single { exhPreferences }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
                *extra,
            )
        }
    }

    fun stop() {
        clearVoyagerScopes()
        stopKoin()
    }
}
