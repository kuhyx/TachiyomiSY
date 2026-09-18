package tachiyomi.data

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import exh.source.MERGED_SOURCE_ID
import tachiyomi.view.LibraryView
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private val mapper = { cursor: SqlCursor ->
    val row = RowReader(cursor)
    LibraryView(
        _id = row.long(),
        source = row.long(),
        url = row.string(),
        artist = row.stringOrNull(),
        author = row.stringOrNull(),
        description = row.stringOrNull(),
        genre = row.stringOrNull()?.let(StringListColumnAdapter::decode),
        title = row.string(),
        status = row.long(),
        thumbnail_url = row.stringOrNull(),
        favorite = row.boolean(),
        last_update = row.longOrNull(),
        next_update = row.longOrNull(),
        initialized = row.boolean(),
        viewer = row.long(),
        chapter_flags = row.long(),
        cover_last_modified = row.long(),
        date_added = row.long(),
        filtered_scanlators = row.skipped(),
        update_strategy = UpdateStrategyColumnAdapter.decode(row.long()),
        calculate_interval = row.long(),
        last_modified_at = row.long(),
        favorite_modified_at = row.longOrNull(),
        version = row.long(),
        is_syncing = row.long(),
        notes = row.string(),
        memo = MemoColumnAdapter.decode(row.bytes()),
        totalCount = row.long(),
        readCount = row.double(),
        latestUpload = row.long(),
        chapterFetchedAt = row.long(),
        lastRead = row.long(),
        bookmarkCount = row.double(),
        categories = row.string(),
    )
}

/** The merged-aware library query on the injected driver; [condition] is the SQL filter on `mangas M`. */
public fun getLibraryQuery(condition: String = DEFAULT_CONDITION): LibraryQuery {
    return LibraryQuery(
        Injekt.get<SqlDriver>(),
        condition,
    )
}

private const val DEFAULT_CONDITION = "M.favorite = 1"

/**
 * The library view across normal and merged sources (SY); the generated
 * `libraryView` cannot express the merged half. Same columns as the view.
 *
 * @property driver The driver the query runs on.
 * @property condition SQL predicate on the `mangas` row (`M`) selecting which manga to list; favourites by default.
 */
public class LibraryQuery(
    public val driver: SqlDriver,
    public val condition: String = DEFAULT_CONDITION,
) : ExecutableQuery<LibraryView>(mapper) {

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
        return driver.executeQuery(
            identifier = null,
            sql = librarySql(condition),
            mapper = mapper,
            parameters = 0,
        )
    }

    override fun toString(): String = "LibraryQuery.sq:get"

    private companion object {
        // Both halves of the UNION select the same columns from `mangas M` joined to
        // chapter aggregates C and categories MC; they differ in how C is keyed.
        const val SELECT_COLUMNS = """
            SELECT
                M.*,
                coalesce(C.total, 0) AS totalCount,
                coalesce(C.readCount, 0) AS readCount,
                coalesce(C.latestUpload, 0) AS latestUpload,
                coalesce(C.fetchedAt, 0) AS chapterFetchedAt,
                coalesce(C.lastRead, 0) AS lastRead,
                coalesce(C.bookmarkCount, 0) AS bookmarkCount,
                coalesce(MC.categories, '0') AS categories
            FROM mangas M
        """
        const val CHAPTER_AGGREGATES = """
                    count(*) AS total,
                    sum(read) AS readCount,
                    coalesce(max(chapters.date_upload), 0) AS latestUpload,
                    coalesce(max(history.last_read), 0) AS lastRead,
                    coalesce(max(chapters.date_fetch), 0) AS fetchedAt,
                    sum(chapters.bookmark) AS bookmarkCount
                FROM chapters
                LEFT JOIN excluded_scanlators
                ON chapters.manga_id = excluded_scanlators.manga_id
                AND chapters.scanlator = excluded_scanlators.scanlator
                LEFT JOIN history
                ON chapters._id = history.chapter_id
        """
        const val CATEGORIES_JOIN = """
            LEFT JOIN (
                SELECT manga_id, group_concat(category_id) AS categories
                FROM mangas_categories
                GROUP BY manga_id
            ) AS MC
            ON MC.manga_id = M._id
        """
        const val NORMAL_JOINS = """
            LEFT JOIN (
                SELECT
                    chapters.manga_id,
                    $CHAPTER_AGGREGATES
                WHERE excluded_scanlators.scanlator IS NULL
                GROUP BY chapters.manga_id
            ) AS C
            ON M._id = C.manga_id
            $CATEGORIES_JOIN
        """
        const val MERGED_JOINS = """
            LEFT JOIN (
                SELECT merged.manga_id,merged.merge_id
                FROM merged
                GROUP BY merged.merge_id
            ) as ME
            ON ME.merge_id = M._id
            LEFT JOIN (
                SELECT
                    ME.merge_id,
                    $CHAPTER_AGGREGATES
                LEFT JOIN merged as ME
                ON ME.manga_id = chapters.manga_id
                WHERE excluded_scanlators.scanlator IS NULL
                GROUP BY ME.merge_id
            ) AS C
            ON ME.merge_id = C.merge_id
            $CATEGORIES_JOIN
        """

        fun librarySql(condition: String): String = """
            $SELECT_COLUMNS
            $NORMAL_JOINS
            WHERE $condition AND M.source <> $MERGED_SOURCE_ID
            UNION
            $SELECT_COLUMNS
            $MERGED_JOINS
            WHERE $condition AND M.source = $MERGED_SOURCE_ID;
        """.trimIndent()
    }
}
