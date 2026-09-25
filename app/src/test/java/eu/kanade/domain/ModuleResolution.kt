package eu.kanade.domain

import android.app.Application
import android.content.Context
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.di.InjektKoinBridge
import eu.kanade.tachiyomi.di.InjektModule
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Database
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.api.InjektRegistrar
import kotlin.io.path.createTempDirectory
import kotlin.reflect.KClass

/** The collaborators the domain modules expect the app module to provide. */
internal fun domainLeaves(): Module = module {
    single<Database> { mockk() }
    single<Application> {
        val filesDir = createTempDirectory("domain-module").toFile()
        mockk(relaxed = true) { every { getExternalFilesDir(null) } returns filesDir }
    }
    single<Context> { get<Application>() }
    single<PreferenceStore> { InMemoryPreferenceStore() }
    single { UiPreferences(get()) }
    single<CoverCache> { mockk() }
    single<PagePreviewCache> { mockk() }
    single<DownloadProvider> { mockk() }
    single<ProtoBuf> { ProtoBuf }
    single<Json> { Json }
    single<NetworkHelper> { mockk(relaxed = true) }
    single<SourceManager> { mockk() }
    single<ExtensionManager> { mockk() }
    single<DownloadManager> { mockk() }
    single<DownloadCache> { mockk() }
    single<TrackerManager> { mockk() }
    single { LibraryPreferences(InMemoryPreferenceStore()) }
    single { DownloadPreferences(InMemoryPreferenceStore()) }
    single { SourcePreferences(InMemoryPreferenceStore()) }
    single { BasePreferences(get(), InMemoryPreferenceStore()) }
    single { TrackPreferences(InMemoryPreferenceStore()) }
    single<DelayedTrackingStore> { mockk() }
}

/**
 * Registers [injektModules] through the bridge and resolves every definition they added against a Koin
 * context that also holds [leaves], so each factory body runs once. Returns the primary types resolved,
 * per module.
 */
@OptIn(KoinInternalApi::class)
internal fun resolveEveryDefinition(injektModules: List<InjektModule>, leaves: Module): List<List<KClass<*>>> {
    val registrar = mockk<InjektRegistrar>()
    injektModules.forEach { with(it) { registrar.registerInjectables() } }
    val modules = injektModules.map { InjektKoinBridge.getModule(it) }
    val koin: Koin = startKoin { modules(modules + leaves) }.koin
    return modules.map { module ->
        module.mappings.values.map { factory ->
            val definition = factory.beanDefinition
            koin.get<Any>(definition.primaryType, definition.qualifier)
            definition.primaryType
        }
    }
}
