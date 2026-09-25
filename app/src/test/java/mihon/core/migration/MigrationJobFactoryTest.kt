package mihon.core.migration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import logcat.LogPriority
import logcat.LogcatLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MigrationJobFactoryTest {

    private val logged = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        LogcatLogger.install()
        LogcatLogger.loggers += object : LogcatLogger {
            @Deprecated("Superseded by the tagged overload", ReplaceWith("isLoggable(priority, \"\")"))
            override fun isLoggable(priority: LogPriority): Boolean = true

            override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

            override fun log(priority: LogPriority, tag: String, message: String) {
                logged += message
            }
        }
    }

    @AfterEach
    fun tearDown() {
        LogcatLogger.uninstall()
    }

    @Test
    fun dryRunSkipsEveryMigration() = runTest {
        val factory = MigrationJobFactory(MigrationContext(dryrun = true, previousVersion = 0), this)
        var ran = false
        val migration = fixedMigration(2f) {
            ran = true
            true
        }
        factory.create(listOf(migration)).await() shouldBe true
        ran shouldBe false
        logged.single() shouldContain "(Dry-run) Running migration"
    }

    @Test
    fun runsInVersionOrder() = runTest {
        val factory = MigrationJobFactory(MigrationContext(dryrun = false, previousVersion = 0), this)
        val order = mutableListOf<Float>()
        val migrations = listOf(
            fixedMigration(3f) {
                order += 3f
                false
            },
            fixedMigration(1f) {
                order += 1f
                false
            },
        )
        factory.create(migrations).await() shouldBe true
        order shouldBe listOf(1f, 3f)
        logged.size shouldBe 2
        logged.first() shouldContain "version = 1.0"
    }

    @Test
    fun noMigrationsSucceeds() = runTest {
        val factory = MigrationJobFactory(MigrationContext(dryrun = false, previousVersion = 0), this)
        factory.create(emptyList()).await() shouldBe true
    }

    @Test
    fun failureKeepsPriorResult() = runTest {
        val factory = MigrationJobFactory(MigrationContext(dryrun = false, previousVersion = 0), this)
        val migrations = listOf(fixedMigration(1f) { true }, fixedMigration(2f) { false })
        factory.create(migrations).await() shouldBe true
    }
}

/** A migration at [version] whose outcome [action] decides. */
internal fun fixedMigration(version: Float, action: suspend (MigrationContext) -> Boolean): Migration =
    object : Migration {
        override val version: Float = version

        override suspend fun invoke(migrationContext: MigrationContext): Boolean = action(migrationContext)
    }
