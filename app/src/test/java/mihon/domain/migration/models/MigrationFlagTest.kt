package mihon.domain.migration.models

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MigrationFlagTest {

    @Test
    fun flagsAreDistinctBits() {
        MigrationFlag.entries.map { it.flag } shouldBe listOf(1, 2, 8, 32, 16)
        MigrationFlag.valueOf("NOTES") shouldBe MigrationFlag.NOTES
    }

    @Test
    fun bitsRoundTrip() {
        MigrationFlag.toBit(emptySet()) shouldBe 0
        MigrationFlag.toBit(setOf(MigrationFlag.CHAPTER, MigrationFlag.NOTES)) shouldBe 33
        MigrationFlag.fromBit(0) shouldBe emptySet()
        MigrationFlag.fromBit(33) shouldBe setOf(MigrationFlag.CHAPTER, MigrationFlag.NOTES)
        MigrationFlag.fromBit(MigrationFlag.toBit(MigrationFlag.entries.toSet())) shouldBe MigrationFlag.entries.toSet()
    }
}
