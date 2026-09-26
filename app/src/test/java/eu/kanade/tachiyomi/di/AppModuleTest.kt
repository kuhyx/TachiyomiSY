package eu.kanade.tachiyomi.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveService
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import exh.eh.EHentaiUpdateHelper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.storage.AndroidStorageFolderProvider
import tachiyomi.core.common.storage.UniFileTempFileManager
import tachiyomi.data.Database
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@RunWith(RobolectricTestRunner::class)
internal class AppModuleTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        stopAppGraph()
    }

    @Test
    fun serializationResolves() {
        startAppGraph(app)
        AppModule(app).app shouldBe app
        Injekt.get<Application>() shouldBeSameInstanceAs app
        Injekt.get<SqlDriver>() shouldBeSameInstanceAs Injekt.get<SqlDriver>()
        Injekt.get<Database>() shouldBeSameInstanceAs Injekt.get<Database>()
        Injekt.get<Json>().configuration.ignoreUnknownKeys shouldBe true
        Injekt.get<XML>().config.isUnchecked shouldBe false
        Injekt.get<ProtoBuf>() shouldBeSameInstanceAs ProtoBuf
    }

    @Test
    fun storageResolves() {
        startAppGraph(app)
        Injekt.get<UniFileTempFileManager>()
        Injekt.get<ChapterCache>()
        Injekt.get<CoverCache>()
        Injekt.get<ImageSaver>()
        Injekt.get<AndroidStorageFolderProvider>()
        Injekt.get<LocalSourceFileSystem>()
        Injekt.get<LocalCoverManager>()
        Injekt.get<StorageManager>()
    }

    // The stub-source repository is the only definition here that starts a database query on its
    // own; the bundled SQLite driver has no JVM native library, so it is served empty.
    @Test
    fun sourcesAndDownloadsResolve() {
        val stubs = mockk<StubSourceRepository> { every { subscribeAll() } returns emptyFlow() }
        startAppGraph(app, module { single { stubs } })
        Injekt.get<NetworkHelper>()
        Injekt.get<JavaScriptEngine>()
        Injekt.get<SourceManager>()
        Injekt.get<ExtensionManager>()
        Injekt.get<DownloadProvider>()
        Injekt.get<DownloadManager>()
        Injekt.get<DownloadCache>()
        Injekt.get<TrackerManager>()
        Injekt.get<DelayedTrackingStore>()
    }

    @Test
    fun syExtrasResolve() {
        startAppGraph(app)
        Injekt.get<EHentaiUpdateHelper>()
        Injekt.get<PagePreviewCache>()
        Injekt.get<GoogleDriveService>()
    }
}
