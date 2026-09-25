package mihon.feature.migration.list

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import mihon.feature.migration.list.models.MigratingManga
import tachiyomi.domain.manga.model.Manga
import kotlin.coroutines.EmptyCoroutineContext

/** A manga as the migration list shows it. */
internal fun listManga(id: Long, title: String): Manga = Manga.create().copy(id = id, ogTitle = title, source = id)

/** An entry being migrated, whose search has reached [result]. */
internal fun migrating(
    id: Long,
    result: MigratingManga.SearchResult = MigratingManga.SearchResult.Searching,
    latestChapter: Double? = 3.0,
): MigratingManga = MigratingManga(
    manga = listManga(id, "Entry $id"),
    chapterCount = 4,
    latestChapter = latestChapter,
    source = "Source $id",
    parentContext = EmptyCoroutineContext,
).also { it.searchResult.value = result }

/** A found match. */
internal fun found(id: Long, latestChapter: Double? = 7.0): MigratingManga.SearchResult.Success =
    MigratingManga.SearchResult.Success(
        manga = listManga(id, "Match $id"),
        chapterCount = 9,
        latestChapter = latestChapter,
        source = "Target $id",
    )

/** Clickable icons without a label: the per-row action buttons. */
internal val bareButton: SemanticsMatcher = hasClickAction() and SemanticsMatcher("has no label") {
    SemanticsProperties.Text !in it.config && SemanticsProperties.ContentDescription !in it.config
}
