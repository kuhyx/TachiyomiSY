package eu.kanade.tachiyomi.data.download

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hippo.unifile.UniFile
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.source.service.SourceManager
import java.io.File

/** Polls [condition] for up to [timeoutMs]: the cache renews on its own IO scope. */
internal fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        check(System.currentTimeMillis() < deadline) { "condition not met in $timeoutMs ms" }
        Thread.sleep(10)
    }
}

internal fun httpSource(name: String, id: Long): HttpSource = mockk<HttpSource>().also {
    every { it.toString() } returns name
    every { it.id } returns id
    every { it.name } returns name
}

/**
 * A [DownloadCache] over a real downloads tree in [root]: two sources ("Alpha" id 1, "Beta" id 2),
 * whose directory names the provider derives from `toString()`.
 */
internal abstract class DownloadCacheTestBase {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    protected val context: Context = ApplicationProvider.getApplicationContext()
    protected val alpha: HttpSource = httpSource(name = "Alpha", id = 1L)
    protected val beta: HttpSource = httpSource(name = "Beta", id = 2L)
    protected val sourceManager: SourceManager = mockk(relaxed = true)
    protected val extensionManager: ExtensionManager = mockk()
    protected val storageChanges = MutableSharedFlow<Unit>()
    protected lateinit var root: File

    /** Every message logged during the test. */
    protected lateinit var logged: MutableList<String>
    protected lateinit var provider: DownloadProviderHarness

    protected val diskCacheFile: File
        get() = File(context.cacheDir, "dl_index_cache_v3")

    @Before
    fun setUpCache() {
        root = tmp.newFolder("downloads")
        diskCacheFile.deleteRecursively()
        provider = DownloadProviderHarness(root = root, context = context)
        provider.downloadPreferences.includeChapterUrlHash.set(false)
        every { provider.storageManager.changes } returns storageChanges
        every { extensionManager.isInitialized } returns MutableStateFlow(true)
        every { sourceManager.isInitialized } returns MutableStateFlow(true)
        every { sourceManager.getVisibleOnlineSources() } returns listOf(alpha)
        every { sourceManager.getStubSources() } returns emptyList()
        every { sourceManager.get(1L) } returns alpha
        every { sourceManager.get(2L) } returns beta
        every { sourceManager.getOrStub(any()) } answers { if (firstArg<Long>() == 2L) beta else alpha }
        logged = captureLogcat()
        startKoin { modules(module { single<Application> { context as Application } }) }
    }

    @After
    fun tearDownCache() {
        releaseLogcat()
        stopKoin()
        diskCacheFile.deleteRecursively()
    }

    protected fun newCache(): DownloadCache = DownloadCache(
        context = context,
        provider = provider.provider,
        sourceManager = sourceManager,
        extensionManager = extensionManager,
        storageManager = provider.storageManager,
    )

    /** Creates `<source>/<manga>/<entry>` under the downloads root. */
    protected fun entry(source: String, manga: String, name: String, file: Boolean = false): File {
        val mangaDir = File(root, "$source/$manga").apply { mkdirs() }
        return File(mangaDir, name).apply { if (file) writeText("zip") else mkdirs() }
    }

    protected fun uni(file: File): UniFile = UniFile.fromFile(file)!!

    internal fun sourceOf(id: Long): Source = if (id == 2L) beta else alpha
}
