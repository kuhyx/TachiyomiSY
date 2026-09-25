package exh.util

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class ThrottleManagerTest {
    @Test
    fun throttleTimeGrowsUpToMax() = runTest {
        val manager = ThrottleManager(max = 30.milliseconds, inc = 20.milliseconds)
        manager.throttleTime shouldBe Duration.ZERO
        manager.throttle()
        manager.throttleTime shouldBe 20.milliseconds
        // Immediately again: the delay branch runs, and the increment reaches the max.
        manager.throttle()
        manager.throttleTime shouldBe 40.milliseconds
        manager.throttle()
        manager.throttleTime shouldBe 40.milliseconds
    }

    @Test
    fun resetRestoresInitial() = runTest {
        val manager = ThrottleManager(max = 100.milliseconds, inc = 10.milliseconds, initial = 5.milliseconds)
        manager.throttle()
        manager.throttleTime shouldBe 15.milliseconds
        manager.resetThrottle()
        manager.throttleTime shouldBe 5.milliseconds
    }

    @Test
    fun defaultsMatchCompanion() = runTest {
        val manager = ThrottleManager()
        manager.throttle()
        manager.throttleTime shouldBe ThrottleManager.THROTTLE_INC
        ThrottleManager.THROTTLE_MAX shouldBe 5500.milliseconds
    }
}
