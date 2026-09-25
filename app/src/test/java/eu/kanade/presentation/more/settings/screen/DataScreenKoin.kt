package eu.kanade.presentation.more.settings.screen

import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.GetFavorites

/** What the data settings screen pulls on top of [SettingsKoin]'s preferences. */
internal class DataScreenKoin {
    val chapterCache: ChapterCache = mockk { every { readableSize } returns "1 MB" }
    val previewCache: PagePreviewCache = mockk { every { readableSize } returns "2 MB" }
    val getFavorites: GetFavorites = mockk { coEvery { await() } returns emptyList() }
    val googleDrive: GoogleDriveService = mockk(relaxed = true)

    fun module(): Module = module {
        single { chapterCache }
        single { previewCache }
        single { getFavorites }
        single { googleDrive }
        single<ProtoBuf> { ProtoBuf }
    }
}
