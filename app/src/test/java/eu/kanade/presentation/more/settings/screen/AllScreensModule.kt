package eu.kanade.presentation.more.settings.screen

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.source.service.SourceManager

/** Everything beyond [SettingsKoin]'s preferences that composing every settings screen at once pulls. */
internal fun allScreensModules(): Array<Module> = arrayOf(
    DataScreenKoin().module(),
    EhScreenKoin().module(),
    AdvancedScreenKoin().module(),
    module {
        single { mockk<GetCategories> { every { subscribe() } returns flowOf(emptyList()) } }
        single { stubTrackerManager(emptyList()) }
        single {
            mockk<SourceManager> {
                every { getAll() } returns emptyList()
                every { getVisibleOnlineSources() } returns emptyList()
            }
        }
        single { mockk<GetExtensionStoreCountAsFlow>().also { every { it() } returns flowOf(0L) } }
    },
)
