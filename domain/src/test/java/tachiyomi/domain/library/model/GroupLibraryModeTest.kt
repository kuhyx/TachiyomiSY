package tachiyomi.domain.library.model

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class GroupLibraryModeTest {

    @Test
    fun entriesInDeclarationOrder() {
        GroupLibraryMode.entries shouldContainExactly listOf(
            GroupLibraryMode.GLOBAL,
            GroupLibraryMode.ALL_BUT_UNGROUPED,
            GroupLibraryMode.ALL,
        )
    }

    @Test
    fun valueOfParsesNames() {
        GroupLibraryMode.entries.forEach { mode ->
            GroupLibraryMode.valueOf(mode.name) shouldBe mode
        }
        GroupLibraryMode.ALL.ordinal shouldBe 2
    }
}
