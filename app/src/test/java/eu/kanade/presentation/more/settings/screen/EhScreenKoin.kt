package eu.kanade.presentation.more.settings.screen

import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.DeleteFavoriteEntries
import tachiyomi.domain.manga.interactor.GetExhFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetFlatMetadataById

/** The interactors the E-Hentai settings screen pulls on top of [SettingsKoin]'s preferences. */
internal class EhScreenKoin {
    val getFlatMetadata: GetFlatMetadataById = mockk()
    val deleteFavorites: DeleteFavoriteEntries = mockk()
    val getExhFavorites: GetExhFavoriteMangaWithMetadata = mockk()

    fun module(): Module = module {
        single { getFlatMetadata }
        single { deleteFavorites }
        single { getExhFavorites }
    }
}
