package mihon.core.migration.migrations

import io.kotest.matchers.shouldBe
import mihon.core.migration.Migration
import org.junit.jupiter.api.Test

internal class MigrationsTest {

    @Test
    fun listsEveryMigrationOnce() {
        val all = migrations
        all.size shouldBe 46
        all.map { it::class }.distinct().size shouldBe all.size
    }

    @Test
    fun fiveMigrationsAlwaysRun() {
        val versions = migrations.map { it.version }
        versions.count { it == Migration.ALWAYS } shouldBe 5
        versions.max() shouldBe 79f
    }
}
