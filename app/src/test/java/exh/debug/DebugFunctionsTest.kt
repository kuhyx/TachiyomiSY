package exh.debug

import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import org.junit.jupiter.api.Test

internal class DebugFunctionsTest {

    @Test
    fun everyGroupContributesEntries() {
        val entries = DebugFunctions.entries()
        entries.size shouldBeGreaterThanOrEqual EXPECTED_ENTRIES
        entries.map { it.function.name }.shouldBeUnique()
        entries.map { it.owner::class }.toSet().size shouldBeGreaterThanOrEqual 5
    }
}

// The 31 public functions the menu listed before the split into topic objects.
private const val EXPECTED_ENTRIES = 31
