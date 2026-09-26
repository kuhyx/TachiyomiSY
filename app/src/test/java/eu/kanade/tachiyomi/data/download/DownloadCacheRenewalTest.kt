package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import java.io.File
import java.io.IOException

/** The renewal job's life cycle: a renewal in flight, a cancelled one and a failed one. */
@RunWith(RobolectricTestRunner::class)
internal class DownloadCacheRenewalTest : DownloadCacheTestBase() {

    @Test
    fun renewalInFlightIsNotRestarted() {
        val extensionsReady = MutableStateFlow(false)
        every { extensionManager.isInitialized } returns extensionsReady
        entry(source = "Alpha", manga = "Title", name = "Ch 1")
        val cache = newCache()
        cache.renewCache()
        waitUntil { runCatching { verify { extensionManager.isInitialized } }.isSuccess }
        cache.renewCache()
        verify(exactly = 1) { extensionManager.isInitialized }
        extensionsReady.value = true
        waitUntil { cache.getTotalDownloadCount() == 1 }
    }

    /**
     * Current behaviour, reported upstream-of-fix: the cancelled job's completion handler stamps
     * `lastRenew`, so the renewal `invalidateCache` asks for right after is skipped as too recent.
     */
    @Test
    fun invalidationCancelsTheRenewal() {
        val extensionsReady = MutableStateFlow(false)
        every { extensionManager.isInitialized } returns extensionsReady
        val cache = newCache()
        cache.renewCache()
        waitUntil { runCatching { verify { extensionManager.isInitialized } }.isSuccess }
        entry(source = "Alpha", manga = "Title", name = "Ch 1")
        cache.invalidateCache()
        extensionsReady.value = true
        Thread.sleep(300)
        cache.getTotalDownloadCount() shouldBe 0
        logged.none { it.contains("failed to create cache") } shouldBe true
    }

    /**
     * Current behaviour: the failure is logged by the completion handler and still escapes the cache's
     * scope as an uncaught exception. It is collected here, inside `runTest`, after joining the renewal,
     * so it cannot surface at a later test's `runTest` as `UncaughtExceptionsBeforeTest`.
     */
    @Test
    fun failedRenewalIsLogged() {
        val downloads = UniFile.fromFile(root)
        var calls = 0
        every { provider.storageManager.getDownloadsDirectory() } answers {
            if (calls++ == 0) downloads else throw IOException("unmounted")
        }
        val cache = newCache()
        val renewal = DownloadCache::class.java.getDeclaredField("renewalJob").apply { isAccessible = true }
        val escaped = shouldThrow<IOException> {
            runTest {
                cache.renewCache()
                (renewal.get(cache) as Job).join()
            }
        }
        escaped.message shouldBe "unmounted"
        logged.any { it.contains("failed to create cache") } shouldBe true
    }

    @Test
    fun invalidatingAFreshCache() {
        entry(source = "Alpha", manga = "Title", name = "Ch 1")
        val cache = newCache()
        cache.invalidateCache()
        waitUntil { cache.rootDownloadsDir.chapterCount() == 1 }
    }

    /** An index older than the renew interval is rebuilt without the "initializing" flag. */
    @Test
    fun staleIndexRenewsQuietly() {
        entry(source = "Alpha", manga = "Title", name = "Ch 1")
        val cache = newCache()
        DownloadCache::class.java.getDeclaredField("lastRenew").apply { isAccessible = true }.setLong(cache, 1L)
        waitUntil { cache.getTotalDownloadCount() == 1 }
        cache.isInitializing.value shouldBe false
    }

    @Test
    fun defaultsComeFromInjekt() {
        loadKoinModules(
            module {
                single { provider.provider }
                single<SourceManager> { sourceManager }
                single { extensionManager }
                single<StorageManager> { provider.storageManager }
            },
        )
        entry(source = "Alpha", manga = "Title", name = "Ch 1")
        val cache = DownloadCache(context)
        waitUntil { cache.getTotalDownloadCount() == 1 }
        File(root, "Alpha").isDirectory shouldBe true
    }
}
