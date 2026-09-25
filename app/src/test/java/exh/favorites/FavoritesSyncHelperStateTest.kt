package exh.favorites

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FavoritesSyncHelperStateTest {
    private val harness = FavoritesSyncHarness()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() {
        scope.cancel()
        harness.stop()
    }

    @Test
    fun busyHelperIgnoresRunSync() {
        val helper = harness.helper()
        helper.status.value = FavoritesSyncStatus.Processing.CleaningUp
        helper.runSync(scope)
        helper.status.value shouldBe FavoritesSyncStatus.Processing.CleaningUp
    }

    @Test
    fun fallsBackToAFreshSource() {
        every { harness.sourceManager.get(any()) } returns null
        val helper = harness.helper()
        helper.exh.exh.shouldBeTrue()
        helper.exh shouldBeSameInstanceAs helper.exh
        helper.throttleManager shouldBeSameInstanceAs helper.throttleManager
        helper.needWarnThrottle().shouldBeFalse()
        runTest { repeat(50) { helper.throttleManager.throttle() } }
        helper.needWarnThrottle().shouldBeTrue()
        val message = FavoritesSyncStatus.SyncError.GallerySyncError.UnableToDeleteFromRemote
        FavoritesSyncHelper.IgnoredException(message).message shouldBe message.toString()
    }
}
