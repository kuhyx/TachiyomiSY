package eu.kanade.tachiyomi.di

import android.content.Context
import logcat.LogPriority
import logcat.logcat
import mihon.core.common.InlinedOnly
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.definition.Definition
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope

internal object InjektKoinBridge {
    private val modules = mutableMapOf<InjektModule, Module>()
    fun getModule(injektModule: InjektModule) = modules.getOrPut(injektModule) { Module() }

    fun startKoin(context: Context) {
        startKoin {
            logger(
                object : Logger() {
                    override fun display(level: Level, msg: MESSAGE) {
                        logcat(
                            when (level) {
                                Level.DEBUG -> LogPriority.DEBUG
                                Level.INFO -> LogPriority.INFO
                                Level.WARNING -> LogPriority.WARN
                                Level.ERROR -> LogPriority.ERROR
                                Level.NONE -> LogPriority.VERBOSE
                            },
                        ) { msg }
                    }
                },
            )
            androidContext(context)
            modules(modules.values.toList())
        }
    }
}

internal interface InjektModule {
    fun InjektRegistrar.registerInjectables()
}

// The reified registrations only name the type: the definitions come from the non-inline
// helpers below, so the compiled copies of the reified stubs hold nothing to measure.

/** The Koin [Module] collecting this module's definitions. */
internal fun InjektModule.koinModule(): Module = InjektKoinBridge.getModule(this)

/** A definition that always answers [instance]. */
internal fun <T> constantDefinition(instance: T): Definition<T> = { instance }

/** A definition that builds its value with [instance]. */
internal fun <T> scopedDefinition(instance: Scope.() -> T): Definition<T> = { instance() }

@InlinedOnly
internal inline fun <reified T> InjektModule.addSingleton(instance: T) {
    koinModule().single<T>(definition = constantDefinition(instance))
}

@InlinedOnly
internal inline fun <reified T> InjektModule.addSingletonFactory(noinline instance: Scope.() -> T) {
    koinModule().single<T>(definition = scopedDefinition(instance))
}

@InlinedOnly
internal inline fun <reified T> InjektModule.addFactory(noinline instance: Scope.() -> T) {
    koinModule().factory<T>(definition = scopedDefinition(instance))
}

internal fun InjektScope.importModule(injektModule: InjektModule) {
    with(injektModule) { registrar.registerInjectables() }
}
