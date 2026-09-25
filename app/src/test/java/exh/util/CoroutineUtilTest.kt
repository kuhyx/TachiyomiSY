package exh.util

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CoroutineUtilTest {
    @Test
    fun passesValuesThroughWhileActive() = runTest {
        flowOf(1, 2, 3).cancellable().toList() shouldBe listOf(1, 2, 3)
    }

    @Test
    fun stopsOnceScopeIsCancelled() = runTest {
        val seen = mutableListOf<Int>()
        val job = launch {
            // asFlow() itself never checks for cancellation, so the check is the operator's.
            listOf(1, 2, 3).asFlow().cancellable().collect {
                seen += it
                cancel()
            }
        }
        job.join()
        job.isCancelled.shouldBeTrue()
        seen shouldBe listOf(1)
    }
}
