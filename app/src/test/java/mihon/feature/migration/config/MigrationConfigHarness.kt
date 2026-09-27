package mihon.feature.migration.config

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.source.service.SourceManager

internal const val CONFIG_WAIT_MS = 20_000L

/** Koin with the collaborators of [MigrationConfigScreenModel] and the source icon lookup. */
internal class MigrationConfigHarness {
    val preferences = SourcePreferences(InMemoryPreferenceStore())
    val sourceManager: SourceManager = mockk()

    /** What [SourceManager.getAll] answers; replace before building a model. */
    var sources: () -> List<Source> = { emptyList() }

    fun start() {
        stopKoin()
        val extensions = mockk<ExtensionManager>()
        every { extensions.installedExtensionsFlow } returns MutableStateFlow(emptyList())
        every { sourceManager.getAll() } answers { sources() }
        startKoin {
            modules(
                module {
                    single { preferences }
                    single { sourceManager }
                    single { extensions }
                },
            )
        }
        preferences.enabledLanguages.set(setOf("en", "ja"))
    }

    fun stop() = stopKoin()

    fun model() = MigrationConfigScreenModel(preferences, sourceManager)
}

/** An online source with [id], [name] and [lang]. */
internal fun httpSource(id: Long, name: String = "Source $id", lang: String = "en"): HttpSource {
    val source = mockk<HttpSource>()
    every { source.id } returns id
    every { source.name } returns name
    every { source.lang } returns lang
    return source
}

/** A plain (non-HTTP) source, which the migration list leaves out. */
internal fun plainSource(id: Long): Source {
    val source = mockk<Source>()
    every { source.id } returns id
    every { source.name } returns "Plain $id"
    every { source.lang } returns "en"
    return source
}

/** Waits until the model has loaded its sources and returns that state. */
internal fun MigrationConfigScreenModel.awaitLoaded(): MigrationConfigScreenModel.State =
    runBlocking { withTimeout(CONFIG_WAIT_MS) { state.first { !it.isLoading } } }

/** The ids of the selected sources, in list order. */
internal fun MigrationConfigScreenModel.State.selectedIds(): List<Long> =
    sources.filter { it.isSelected }.map { it.id }
