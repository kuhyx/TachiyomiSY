package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.MangaRepository

/** Clears every manga's per-manga reader settings (the advanced-settings "reset viewer flags" action). */
public class ResetViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    /** Clears every manga's viewer flags; true on success. */
    public suspend fun await(): Boolean = mangaRepository.resetViewerFlags()
}
