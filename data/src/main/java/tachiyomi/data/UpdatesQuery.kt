package tachiyomi.data

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import exh.source.MERGED_SOURCE_ID
import tachiyomi.view.UpdatesView
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private val mapper = { cursor: SqlCursor ->
    val row = RowReader(cursor)
    UpdatesView(
        mangaId = row.long(),
        mangaTitle = row.string(),
        chapterId = row.long(),
        chapterName = row.string(),
        scanlator = row.stringOrNull(),
        chapterUrl = row.string(),
        read = row.boolean(),
        bookmark = row.boolean(),
        last_page_read = row.long(),
        source = row.long(),
        favorite = row.boolean(),
        thumbnailUrl = row.stringOrNull(),
        coverLastModified = row.long(),
        dateUpload = row.long(),
        datefetch = row.long(),
        excludedScanlator = row.stringOrNull(),
    )
}

/**
 * The filters of the updates screen, as the SQL binds them.
 *
 * @property after Only chapters uploaded after this epoch millis.
 * @property limit Row limit.
 * @property read Null for any, else the `read` column value.
 * @property started Null for any, 1 for chapters in progress, 0 for chapters not opened.
 * @property bookmarked Null for any, else the `bookmark` column value.
 * @property hideExcludedScanlators 1 to drop chapters from a manga's excluded scanlators.
 */
public data class UpdatesFilter(
    val after: Long,
    val limit: Long,
    val read: Boolean?,
    val started: Long?,
    val bookmarked: Boolean?,
    val hideExcludedScanlators: Long,
)

/** The merged-aware updates query for [filter] on the injected driver. */
public fun getUpdatesQuery(filter: UpdatesFilter): UpdatesQuery = UpdatesQuery(Injekt.get<SqlDriver>(), filter)

/**
 * The updates list across normal and merged sources (SY); the generated
 * `updatesView` cannot express the merged half. Same columns as the view.
 *
 * @property driver The driver the query runs on.
 * @property filter The date and chapter-state filters bound as the query parameters.
 */
public class UpdatesQuery(
    public val driver: SqlDriver,
    public val filter: UpdatesFilter,
) : ExecutableQuery<UpdatesView>(mapper) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
        return driver.executeQuery(
            identifier = null,
            sql = updatesSql(),
            mapper = mapper,
            parameters = PARAMETER_COUNT,
            binders = {
                var parameterIndex = 0
                bindLong(parameterIndex++, filter.after)
                bindBoolean(parameterIndex++, filter.read)
                bindLong(parameterIndex++, filter.started)
                bindBoolean(parameterIndex++, filter.bookmarked)
                bindLong(parameterIndex++, filter.hideExcludedScanlators)
                bindLong(parameterIndex++, filter.limit)
            },
        )
    }

    override fun toString(): String = "LibraryQuery.sq:get"

    private companion object {
        const val PARAMETER_COUNT = 6

        // Both halves of the UNION select the same columns; they differ in how chapters join mangas.
        const val SELECT_COLUMNS = """
                SELECT
                    mangas._id AS mangaId,
                    mangas.title AS mangaTitle,
                    chapters._id AS chapterId,
                    chapters.name AS chapterName,
                    chapters.scanlator,
                    chapters.url AS chapterUrl,
                    chapters.read,
                    chapters.bookmark,
                    chapters.last_page_read,
                    mangas.source,
                    mangas.favorite,
                    mangas.thumbnail_url AS thumbnailUrl,
                    mangas.cover_last_modified AS coverLastModified,
                    chapters.date_upload AS dateUpload,
                    chapters.date_fetch AS datefetch,
                    excluded_scanlators.scanlator AS excludedScanlator
                FROM mangas
        """
        const val NORMAL_JOINS = """
                JOIN chapters
                    ON mangas._id = chapters.manga_id
                LEFT JOIN excluded_scanlators
                    ON mangas._id = excluded_scanlators.manga_id
                    AND chapters.scanlator = excluded_scanlators.scanlator
        """
        const val MERGED_JOINS = """
                LEFT JOIN (
                    SELECT merged.manga_id, merged.merge_id
                    FROM merged
                    GROUP BY merged.merge_id
                ) AS ME
                    ON ME.merge_id = mangas._id
                JOIN chapters
                    ON ME.manga_id = chapters.manga_id
                LEFT JOIN excluded_scanlators
                    ON ME.merge_id = excluded_scanlators.manga_id
                    AND chapters.scanlator = excluded_scanlators.scanlator
        """
        const val FILTERS = """
            WHERE
                favorite = 1
                AND dateUpload > :after
                AND (:read IS NULL OR read = :read)
                AND (
                    :started IS NULL
                    OR (:started = 1 AND last_page_read > 0 AND read = 0)
                    OR (:started = 0 AND last_page_read = 0 AND read = 0)
                )
                AND (:bookmarked IS NULL OR bookmark = :bookmarked)
                AND (
                    excludedScanlator IS NULL OR :hideExcludedScanlators = 0
                )
            ORDER BY datefetch DESC
            LIMIT :limit;
        """

        fun updatesSql(): String = """
            SELECT *
            FROM (
                -- Normal source
                $SELECT_COLUMNS
                $NORMAL_JOINS
                WHERE mangas.source <> $MERGED_SOURCE_ID
                AND date_fetch > date_added

                UNION ALL

                -- Merged source
                $SELECT_COLUMNS
                $MERGED_JOINS
                WHERE mangas.source = $MERGED_SOURCE_ID
                AND date_fetch > date_added
            ) AS combined
            $FILTERS
        """.trimIndent()
    }
}
