package eu.kanade.tachiyomi

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.di.InjektKoinBridge
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.GLUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import logcat.LogcatLogger
import mihon.core.migration.Migrator
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.data.Database
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.updates.interactor.GetUpdates

/**
 * Runs the real [App.onCreate] over the real Koin graph, with the few collaborators that would
 * touch the database, the network or the GPU replaced once the app has started Koin.
 */
internal class AppBoot {
    val app: App = attachedApp()
    var migrationsFinished: Boolean = false
        private set

    /** The process name [App.onCreate] sees; null keeps the sandbox's own. */
    var processName: String? = null
    private val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

    private val overrides = module {
        single { mockk<GetUpdates> { every { subscribe(read = any(), after = any()) } returns flowOf(emptyList()) } }
        single { mockk<StorageManager> { every { getLogsDirectory() } returns null } }
        single { mockk<NetworkHelper>(relaxed = true) }
        single { mockk<SourceManager>(relaxed = true) }
        single { mockk<Database>(relaxed = true) }
        single { mockk<DownloadManager>(relaxed = true) }
        single { mockk<GetCustomMangaInfo>(relaxed = true) }
    }

    fun create(): App {
        mockkObject(InjektKoinBridge, Migrator, GLUtil)
        every { InjektKoinBridge.startKoin(any()) } answers {
            callOriginal()
            loadKoinModules(overrides)
        }
        every { Migrator.initialize(any(), any(), any(), any(), any()) } answers {
            lastArg<() -> Unit>().invoke()
            migrationsFinished = true
        }
        every { GLUtil.DEVICE_TEXTURE_LIMIT } returns 4096
        processName?.let { name ->
            mockkStatic(Application::class)
            every { Application.getProcessName() } returns name
        }
        // Android's RuntimeInit always installs a default handler before
        // Application.onCreate; a bare test JVM has none.
        if (Thread.getDefaultUncaughtExceptionHandler() == null) {
            Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
        }
        app.onCreate()
        return app
    }

    fun close() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(app)
        app.disableIncognitoReceiver.unregister()
        stopKoin()
        unmockkAll()
        LogcatLogger.loggers.clear()
        LogcatLogger.uninstall()
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }
}
