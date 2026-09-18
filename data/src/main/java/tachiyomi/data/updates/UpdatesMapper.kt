package tachiyomi.data.updates

import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.view.UpdatesView

/** Domain models from the updates view rows. */
public object UpdatesMapper {
    /** The [UpdatesWithRelations] of an updates view row. */
    public fun mapUpdates(row: UpdatesView): UpdatesWithRelations = UpdatesWithRelations(
        mangaId = row.mangaId,
        // SY -->
        ogMangaTitle = row.mangaTitle,
        // SY <--
        chapterId = row.chapterId,
        chapterName = row.chapterName,
        scanlator = row.scanlator,
        chapterUrl = row.chapterUrl,
        read = row.read,
        bookmark = row.bookmark,
        lastPageRead = row.last_page_read,
        sourceId = row.source,
        dateFetch = row.datefetch,
        coverData = MangaCover(
            mangaId = row.mangaId,
            sourceId = row.source,
            isMangaFavorite = row.favorite,
            ogUrl = row.thumbnailUrl,
            lastModified = row.coverLastModified,
        ),
    )
}
