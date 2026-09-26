package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.tachiyomi.source.AndroidSourceManager.Companion.DELEGATED_SOURCES
import eu.kanade.tachiyomi.source.AndroidSourceManager.Companion.DelegatedSource
import eu.kanade.tachiyomi.source.AndroidSourceManager.Companion.currentDelegatedSources
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.MergedSource
import exh.log.xLogD
import exh.source.BlacklistedSources
import exh.source.DelegatedHttpSource
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.EnhancedHttpSource
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import tachiyomi.domain.source.model.StubSource
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

// launchIn rather than launch { collectLatest }: the StateFlow never completes, so a hand-written
// launch block would end on a line no run can reach. mapLatest + buffer(0) is collectLatest.
internal fun AndroidSourceManager.observeExtensions() {
    extensionManager.installedExtensionsFlow
        // SY -->
        .combine(exhPreferences.enableExhentai.changes()) { extensions, enableExhentai ->
            extensions to enableExhentai
        }
        // SY <--
        .mapLatest { (extensions, enableExhentai) ->
            val mutableMap: ConcurrentHashMap<Long, Source> = ConcurrentHashMap<Long, Source>(
                mapOf(
                    LocalSource.ID to LocalSource(
                        context,
                        Injekt.get(),
                        Injekt.get(),
                        // SY -->
                        sourcePreferences.allowLocalSourceHiddenFolders::get,
                        // SY <--
                    ),
                ),
            )

            mutableMap.apply {
                // SY -->
                put(EH_SOURCE_ID, EHentai(EH_SOURCE_ID, false, context))
                if (enableExhentai) {
                    put(EXH_SOURCE_ID, EHentai(EXH_SOURCE_ID, true, context))
                }
                put(MERGED_SOURCE_ID, MergedSource())
                // SY <--
            }

            extensions.forEach { extension ->
                extension.sources.mapNotNull { toInternalSource(it) }.forEach {
                    mutableMap[it.id] = it
                    registerStubSource(StubSource.from(it))
                }
            }
            sourcesMapFlow.value = mutableMap
            markInitialized()
        }
        .buffer(0)
        .launchIn(scope)
}

internal fun AndroidSourceManager.observeStubSources() {
    scope.launch {
        sourceRepository.subscribeAll()
            .collectLatest { sources ->
                val mutableMap = stubSourcesMap.toMutableMap()
                sources.forEach {
                    mutableMap[it.id] = it
                }
            }
    }
}

internal fun AndroidSourceManager.toInternalSource(source: Source): Source? {
    // EXH -->
    val sourceQName = source::class.qualifiedName
    val factories = DELEGATED_SOURCES.entries
        .filter { it.value.factory }
        .map { it.value.originalSourceQualifiedClassName }
    // The source's own name travels with its delegate: a match implies the name is known.
    val match = sourceQName?.let { qualifiedName ->
        val matched = factories.find { qualifiedName.startsWith(it) }
        DELEGATED_SOURCES[matched ?: qualifiedName]?.let { qualifiedName to it }
    }
    val newSource = if (source is HttpSource && match != null) {
        val (qualifiedName, delegate) = match
        xLogD("Delegating source: %s -> %s!", sourceQName, delegate.newSourceClass.qualifiedName)
        val enhancedSource = EnhancedHttpSource(
            source,
            // Every delegated source is built from the original source and the context.
            delegate.newSourceClass.java.getConstructor(HttpSource::class.java, Context::class.java)
                .newInstance(source, context),
        )

        currentDelegatedSources[enhancedSource.originalSource.id] = DelegatedSource(
            enhancedSource.originalSource.name,
            enhancedSource.originalSource.id,
            qualifiedName,
            (enhancedSource.enhancedSource as DelegatedHttpSource)::class,
            delegate.factory,
        )
        enhancedSource
    } else {
        source
    }

    return if (source.id in BlacklistedSources.BLACKLISTED_EXT_SOURCES) {
        xLogD(
            "Removing blacklisted source: (id: %s, name: %s, lang: %s)!",
            source.id,
            source.name,
            source.lang,
        )
        null
    } else {
        newSource
    }
    // EXH <--
}
