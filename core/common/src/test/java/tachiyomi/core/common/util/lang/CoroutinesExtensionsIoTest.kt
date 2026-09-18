package tachiyomi.core.common.util.lang

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@OptIn(DelicateCoroutinesApi::class)
internal class CoroutinesExtensionsIoTest {
    @Test
    fun appScopeIsAliveAndSupervised() {
        AppScope.isActive shouldBe true
        AppScope.coroutineContext[Job].shouldBeInstanceOf<CompletableJob>().isActive shouldBe true
    }

    @Test
    fun appScopeLaunchIoRunsBlock() {
        val ran = CompletableDeferred<Boolean>()
        launchIO { ran.complete(true) }
        runBlocking { ran.await() } shouldBe true
    }

    @Test
    fun scopeLaunchIoRunsBlock() {
        runBlocking {
            var hasRun = false
            launchIO { hasRun = true }.join()
            hasRun shouldBe true
        }
    }

    @Test
    fun nonCancellableOutlivesCancel() {
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var hasFinished = false
            val job = launchNonCancellable {
                started.complete(Unit)
                release.await()
                hasFinished = true
            }
            started.await()
            job.cancel()
            release.complete(Unit)
            job.join()
            hasFinished shouldBe true
        }
    }

    @Test
    fun withIoContextReturnsValue() {
        runBlocking { withIOContext { 1 + 1 } } shouldBe 2
    }

    @Test
    fun withNonCancellableReturnsValue() {
        runBlocking { withNonCancellableContext { "ok" } } shouldBe "ok"
    }
}
