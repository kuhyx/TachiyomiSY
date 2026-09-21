package exh.debug

import eu.kanade.tachiyomi.source.AndroidSourceManager
import eu.kanade.tachiyomi.source.Source
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/** Source listings. Listed in the debug menu through [DebugFunctions]. */
@Suppress("unused")
internal object DebugSourceFunctions {
    private val sourceManager: SourceManager by injectLazy()

    fun getDelegatedSourceList(): String = AndroidSourceManager.currentDelegatedSources.map {
        it.value.sourceName + " : " + it.value.sourceId + " : " + it.value.factory
    }.joinToString(separator = "\n")

    fun listAllSources() = sourceManager.getAll().joinToString("\n") {
        describe(it)
    }

    fun listAllSourcesClassName() = sourceManager.getAll().joinToString("\n") {
        "${it::class.qualifiedName}: ${it.name} (${it.lang.uppercase()})"
    }

    fun listVisibleSources() = sourceManager.getVisibleSources().joinToString("\n") {
        describe(it)
    }

    fun listAllHttpSources() = sourceManager.getOnlineSources().joinToString("\n") {
        describe(it)
    }

    fun listVisibleHttpSources() = sourceManager.getVisibleOnlineSources().joinToString("\n") {
        describe(it)
    }
}

internal fun describe(source: Source) = "${source.id}: ${source.name} (${source.lang.uppercase()})"
