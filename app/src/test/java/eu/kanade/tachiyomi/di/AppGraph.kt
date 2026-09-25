package eu.kanade.tachiyomi.di

import android.app.Application
import eu.kanade.domain.DomainModule
import eu.kanade.domain.SYDomainModule
import io.mockk.unmockkAll
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import uy.kohesive.injekt.Injekt

/**
 * The app's own Koin graph, as [eu.kanade.tachiyomi.App.onCreate] imports it, started with only
 * these modules (the bridge's map is shared by every test in the sandbox) plus [extra] overrides.
 */
internal fun startAppGraph(app: Application, vararg extra: Module) {
    val modules = listOf(
        PreferenceModule(app),
        AppModule(app),
        DomainModule(),
        SYPreferenceModule(app),
        SYDomainModule(),
    )
    modules.forEach { Injekt.importModule(it) }
    startKoin { modules(modules.map { it.koinModule() } + extra) }
}

internal fun stopAppGraph() {
    stopKoin()
    unmockkAll()
}
