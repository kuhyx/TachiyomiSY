package tachiyomi.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

/** A fresh in-memory JDBC SQLite driver with foreign keys on and the full schema applied. */
internal fun inMemoryDriver(): SqlDriver = JdbcSqliteDriver(
    url = JdbcSqliteDriver.IN_MEMORY,
    properties = Properties().apply { put("foreign_keys", "true") },
    schema = Database.Schema.synchronous(),
)

/** A [Database] on [driver] with the production column adapters. */
internal fun databaseOn(driver: SqlDriver): Database = Database(
    driver = driver,
    historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
    mangasAdapter = Mangas.Adapter(
        genreAdapter = StringListColumnAdapter,
        update_strategyAdapter = UpdateStrategyColumnAdapter,
        memoAdapter = MemoColumnAdapter,
    ),
    chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
)

/** A fresh in-memory [Database] with the production column adapters and the full schema applied. */
internal fun inMemoryDatabase(): Database = databaseOn(inMemoryDriver())
