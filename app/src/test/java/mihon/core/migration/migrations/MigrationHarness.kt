package mihon.core.migration.migrations

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import mihon.core.migration.MigrationContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/** A migration context that is not a dry run and reports [previousVersion]. */
internal fun migrationContext(previousVersion: Int = 0): MigrationContext =
    MigrationContext(dryrun = false, previousVersion = previousVersion)

/** Starts the global Koin context with the definitions in [block]; pairs with [stopMigrationKoin]. */
internal fun startMigrationKoin(block: Module.() -> Unit) {
    startKoin { modules(module(moduleDeclaration = block)) }
}

/** Stops the global Koin context started by [startMigrationKoin]. */
internal fun stopMigrationKoin() {
    stopKoin()
}

/** The Robolectric application. */
internal fun robolectricApp(): Application = ApplicationProvider.getApplicationContext()

/** The default shared preferences of [app], where the legacy settings the migrations read live. */
internal fun defaultPrefs(app: Application): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
