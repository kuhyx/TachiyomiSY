package mihon.core.migration

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class MigratorObjectTest {

    @AfterEach
    fun tearDown() {
        Migrator.release()
    }

    @Test
    fun awaitIsFalseBeforeInitialize() = runTest {
        Migrator.await() shouldBe false
    }

    @Test
    fun initialInstallRunsAlways() {
        var completed = 0
        Migrator.initialize(
            old = 0,
            new = 3,
            migrations = listOf(fixedMigration(Migration.ALWAYS) { true }, fixedMigration(2f) { true }),
            onMigrationComplete = { completed++ },
        )
        Migrator.awaitAndRelease() shouldBe true
    }

    @Test
    fun sameVersionIsANoop() = runTest {
        Migrator.initialize(old = 3, new = 3, migrations = listOf(fixedMigration(3f) { true })) { }
        Migrator.await() shouldBe false
        Migrator.release()
        Migrator.await() shouldBe false
    }

    @Test
    fun dryRunNeverInvokesMigrations() {
        var ran = false
        Migrator.initialize(
            old = 1,
            new = 2,
            migrations = listOf(
                fixedMigration(2f) {
                    ran = true
                    true
                },
            ),
            dryrun = true,
            onMigrationComplete = { },
        )
        Migrator.awaitAndRelease() shouldBe true
        ran shouldBe false
    }
}
