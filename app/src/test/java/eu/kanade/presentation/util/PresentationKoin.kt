package eu.kanade.presentation.util

import eu.kanade.domain.ui.UiPreferences
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import eu.kanade.domain.FlowPreferenceStore
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository

/**
 * What the presentation composables pull out of Injekt: real in-memory preferences and a custom-info
 * lookup that knows no edits. Tests add their own collaborators through [start]'s modules.
 */
internal class PresentationKoin {
    val store: FlowPreferenceStore = FlowPreferenceStore()
    val uiPreferences: UiPreferences = UiPreferences(store)

    fun start(vararg extra: Module) {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { uiPreferences }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
                *extra,
            )
        }
    }

    fun stop() {
        stopKoin()
    }

    private object NoCustomInfo : CustomMangaRepository {
        override fun get(mangaId: Long): CustomMangaInfo? = null

        override fun set(mangaInfo: CustomMangaInfo) = Unit
    }
}
