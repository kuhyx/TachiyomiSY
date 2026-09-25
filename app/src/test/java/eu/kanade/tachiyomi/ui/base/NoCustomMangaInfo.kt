package eu.kanade.tachiyomi.ui.base

import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository

/** No user edits for any manga; `Manga`, `MangaCover` and friends read it through Injekt. */
internal object NoCustomMangaInfo : CustomMangaRepository {
    override fun get(mangaId: Long): CustomMangaInfo? = null

    override fun set(mangaInfo: CustomMangaInfo) = Unit
}

/** The Koin module the domain models need before any of their custom-info lookups. */
internal fun customInfoModule(): Module = module { single { GetCustomMangaInfo(NoCustomMangaInfo) } }
