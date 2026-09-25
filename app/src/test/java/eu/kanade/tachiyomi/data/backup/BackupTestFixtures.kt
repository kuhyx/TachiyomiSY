package eu.kanade.tachiyomi.data.backup

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import kotlinx.serialization.json.JsonObject
import logcat.LogPriority
import logcat.LogcatLogger
import tachiyomi.data.Chapters
import tachiyomi.domain.manga.model.Manga

/**
 * Both halves of a driverless query: the cursor that walks [rows] and the row mapper that returns
 * the row the cursor is on. sqldelight's `awaitAsList` drives the cursor and calls the query's own
 * mapper, so feeding both from one index needs no fake column data.
 */
internal class RowFeed<T : Any>(private val rows: List<T>) : SqlCursor, (SqlCursor) -> T {
    private var index = -1

    /** Restarts the walk, so one query can be executed more than once. */
    fun rewind() {
        index = -1
    }

    override fun next(): QueryResult<Boolean> {
        index++
        return QueryResult.Value(index < rows.size)
    }

    override fun invoke(cursor: SqlCursor): T = rows[index]

    override fun getString(index: Int): String? = null

    override fun getLong(index: Int): Long? = null

    override fun getBytes(index: Int): ByteArray? = null

    override fun getDouble(index: Int): Double? = null

    override fun getBoolean(index: Int): Boolean? = null
}

/** A sqldelight [Query] that produces its rows from [feed] instead of a driver. */
internal class FakeQuery<T : Any>(private val feed: RowFeed<T>) : Query<T>(feed) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
        feed.rewind()
        return mapper(feed)
    }

    override fun addListener(listener: Listener) = Unit

    override fun removeListener(listener: Listener) = Unit
}

/** A [Query] whose `awaitAsList`/`awaitAsOne` yield [rows]. */
internal fun <T : Any> fakeQuery(rows: List<T>): Query<T> = FakeQuery(RowFeed(rows))

/** Collects every message the code under test logs, and keeps `logcat { ... }` lambdas running. */
internal class BackupLogger : LogcatLogger {
    val messages: MutableList<String> = mutableListOf()

    override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

    override fun log(priority: LogPriority, tag: String, message: String) {
        messages += message
    }
}

/** Installs a [BackupLogger]; without one, `logcat { ... }` never evaluates its message lambda. */
internal fun installBackupLogger(): BackupLogger {
    val logger = BackupLogger()
    if (!LogcatLogger.isInstalled) {
        LogcatLogger.install()
    }
    LogcatLogger.loggers += logger
    return logger
}

/** Undoes [installBackupLogger]. */
internal fun removeBackupLogger(logger: BackupLogger) {
    LogcatLogger.loggers -= logger
    if (LogcatLogger.loggers.isEmpty()) {
        LogcatLogger.uninstall()
    }
}

/** A [Chapters] row with only the columns the backup code reads set. */
internal fun chapterRow(
    id: Long = 1L,
    mangaId: Long = 1L,
    url: String = "/c",
    name: String = "C",
    version: Long = 0L,
): Chapters = Chapters(
    _id = id,
    manga_id = mangaId,
    url = url,
    name = name,
    scanlator = null,
    read = false,
    bookmark = false,
    last_page_read = 0L,
    chapter_number = 1.0,
    source_order = 0L,
    date_fetch = 0L,
    date_upload = 0L,
    last_modified_at = 0L,
    version = version,
    is_syncing = 0L,
    memo = JsonObject(emptyMap()),
)

/** A library entry with the fields the backup creators read set to distinguishable values. */
internal fun libraryManga(
    id: Long = 1L,
    url: String = "/m",
    source: Long = 3L,
    favorite: Boolean = true,
): Manga = Manga.create().copy(
    id = id,
    url = url,
    source = source,
    favorite = favorite,
    ogTitle = "Title$id",
)
