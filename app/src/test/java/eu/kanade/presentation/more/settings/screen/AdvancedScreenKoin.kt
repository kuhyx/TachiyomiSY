package eu.kanade.presentation.more.settings.screen

import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.ResetViewerFlags

/** What the advanced settings screen pulls on top of [SettingsKoin]'s preferences. */
internal class AdvancedScreenKoin {
    val network: NetworkHelper = mockk(relaxed = true)
    val downloadCache: DownloadCache = mockk(relaxed = true)
    val trust: TrustExtension = mockk(relaxed = true)
    val resetViewerFlags: ResetViewerFlags = mockk()

    fun module(): Module = module {
        single { network }
        single { downloadCache }
        single { trust }
        single { resetViewerFlags }
    }
}
