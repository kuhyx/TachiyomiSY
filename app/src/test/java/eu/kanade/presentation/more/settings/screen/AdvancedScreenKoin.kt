package eu.kanade.presentation.more.settings.screen

import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.GLUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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

/** GL is not there under Robolectric: the texture limits the reader group reads are pinned to the safe value. */
internal fun stubTextureLimits() {
    mockkObject(GLUtil)
    every { GLUtil.DEVICE_TEXTURE_LIMIT } returns GLUtil.SAFE_TEXTURE_LIMIT
    every { GLUtil.CUSTOM_TEXTURE_LIMIT_OPTIONS } returns listOf(GLUtil.SAFE_TEXTURE_LIMIT)
}
