package eu.kanade.tachiyomi.di

import android.app.Application
import android.database.Cursor
import android.os.Looper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.data.Database
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@RunWith(RobolectricTestRunner::class)
internal class AppModuleDriverTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        stopAppGraph()
    }

    @Test
    fun encryptedDatabaseUsesSqlcipher() {
        mockkObject(SqlCipher)
        every { SqlCipher.loadLibrary() } just runs
        every { SqlCipher.password() } returns ByteArray(32) { 1 }
        startAppGraph(app)
        Injekt.get<SecurityPreferences>().encryptDatabase.set(true)
        Injekt.get<SqlDriver>().shouldBeInstanceOf<AndroidSqliteDriver>()
        verify(exactly = 1) { SqlCipher.loadLibrary() }
    }

    @Test
    fun callbackSetsThePragmas() {
        val cursor = mockk<Cursor>(relaxed = true)
        val db = mockk<SupportSQLiteDatabase> { every { query(any<String>()) } returns cursor }
        SqlCipherCallback().onOpen(db)
        verify { db.query("PRAGMA foreign_keys = ON") }
        verify { db.query("PRAGMA journal_mode = WAL") }
        verify { db.query("PRAGMA synchronous = NORMAL") }
        verify(exactly = 3) { cursor.moveToFirst() }
        verify(exactly = 3) { cursor.close() }
    }

    @Test
    fun expensiveComponentsWarmUp() {
        val network = mockk<NetworkHelper>()
        val sources = mockk<SourceManager>()
        val database = mockk<Database>()
        val downloads = mockk<DownloadManager>()
        val customInfo = mockk<GetCustomMangaInfo>()
        startAppGraph(
            app,
            module {
                single { network }
                single { sources }
                single { database }
                single { downloads }
                single { customInfo }
            },
        )
        initExpensiveComponents(app)
        shadowOf(Looper.getMainLooper()).idle()
        verify(exactly = 0) { network.client }
    }
}
