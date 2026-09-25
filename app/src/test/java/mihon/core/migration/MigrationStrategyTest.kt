package mihon.core.migration

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class MigrationStrategyTest {

    @Test
    fun noopReportsItsState() = runTest {
        NoopMigrationStrategy(true)(listOf(fixedMigration(1f) { false })).await() shouldBe true
        NoopMigrationStrategy(false)(emptyList()).await() shouldBe false
    }

    @Test
    fun defaultNotifiesOnSuccess() = runTest {
        var notified = 0
        val factory = MigrationJobFactory(MigrationContext(dryrun = false, previousVersion = 0), this)
        val strategy = DefaultMigrationStrategy(factory, { notified++ }, this)
        strategy(emptyList()).await() shouldBe false
        strategy(listOf(fixedMigration(1f) { true })).await() shouldBe true
        testScheduler.advanceUntilIdle()
        notified shouldBe 1
    }

    @Test
    fun defaultNotifiesDespiteFailure() = runTest {
        var notified = 0
        val context = MigrationContext(dryrun = false, previousVersion = 0)
        val strategy = DefaultMigrationStrategy(MigrationJobFactory(context, this), { notified++ }, this)
        // The chain starts from `true`, so a single failing migration still completes successfully.
        strategy(listOf(fixedMigration(1f) { false })).await() shouldBe true
        testScheduler.advanceUntilIdle()
        notified shouldBe 1
    }

    @Test
    fun initialStrategyKeepsOnlyAlways() = runTest {
        val factory = MigrationJobFactory(MigrationContext(dryrun = false, previousVersion = 0), this)
        val strategy = InitialMigrationStrategy(DefaultMigrationStrategy(factory, { }, this))
        val ran = mutableListOf<Float>()
        val migrations = listOf(
            fixedMigration(Migration.ALWAYS) {
                ran += Migration.ALWAYS
                true
            },
            fixedMigration(2f) {
                ran += 2f
                true
            },
        )
        strategy(migrations).await() shouldBe true
        ran shouldBe listOf(Migration.ALWAYS)
    }

    @Test
    fun rangeKeepsAlwaysAndInRange() = runTest {
        val factory = MigrationJobFactory(MigrationContext(dryrun = false, previousVersion = 0), this)
        val strategy = VersionRangeMigrationStrategy(2..3, DefaultMigrationStrategy(factory, { }, this))
        val ran = mutableListOf<Float>()
        val migrations = listOf(1f, Migration.ALWAYS, 2f, 3f, 4f).map { version ->
            fixedMigration(version) {
                ran += version
                true
            }
        }
        strategy(migrations).await() shouldBe true
        ran shouldBe listOf(Migration.ALWAYS, 2f, 3f)
    }
}
