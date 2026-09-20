package mihon.core.migration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

internal interface MigrationStrategy {
    operator fun invoke(migrations: List<Migration>): Deferred<Boolean>
}

internal class DefaultMigrationStrategy(
    private val migrationJobFactory: MigrationJobFactory,
    private val migrationCompletedListener: MigrationCompletedListener,
    private val scope: CoroutineScope,
) : MigrationStrategy {

    override operator fun invoke(migrations: List<Migration>): Deferred<Boolean> = with(scope) {
        if (migrations.isEmpty()) {
            return@with CompletableDeferred(false)
        }

        val chain = migrationJobFactory.create(migrations)

        launch {
            if (chain.await()) migrationCompletedListener()
        }.start()

        chain
    }
}

internal class InitialMigrationStrategy(private val strategy: DefaultMigrationStrategy) : MigrationStrategy {

    override operator fun invoke(migrations: List<Migration>): Deferred<Boolean> {
        return strategy(migrations.filter { it.isAlways })
    }
}

internal class NoopMigrationStrategy(val state: Boolean) : MigrationStrategy {

    override fun invoke(migrations: List<Migration>): Deferred<Boolean> {
        return CompletableDeferred(state)
    }
}

internal class VersionRangeMigrationStrategy(
    private val versions: IntRange,
    private val strategy: DefaultMigrationStrategy,
) : MigrationStrategy {

    override operator fun invoke(migrations: List<Migration>): Deferred<Boolean> {
        return strategy(migrations.filter { it.isAlways || it.version.toInt() in versions })
    }
}
