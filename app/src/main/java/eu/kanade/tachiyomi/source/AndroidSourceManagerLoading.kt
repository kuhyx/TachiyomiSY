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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.domain.source.model.StubSource
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

internal fun AndroidSourceManager.observeExtensions() {
    extensionManager.installedExtensionsFlow
        // SY -->
        .combine(exhPreferences.enableExhentai.changes()) { extensions, enableExhentai ->
            extensions to enableExhentai
        }
        // SY <--
        .onEach { (extensions, enableExhentai) ->
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
        .launchIn(scope)
}

// Stored stub sources join the map as the repository reports them.
internal fun AndroidSourceManager.observeStubSources() {
    sourceRepository.subscribeAll()
        .onEach { sources -> sources.forEach { stubSourcesMap[it.id] = it } }
        .launchIn(scope)
}

internal fun AndroidSourceManager.toInternalSource(source: Source): Source? {
    // EXH -->
    val sourceQName = source::class.qualifiedName
    val factories = DELEGATED_SOURCES.entries
        .filter { it.value.factory }
        .map { it.value.originalSourceQualifiedClassName }
    val delegate = sourceQName?.let { qualifiedName ->
        val matched = factories.find { qualifiedName.startsWith(it) }
        DELEGATED_SOURCES[matched ?: qualifiedName]
    }
    val newSource = if (source is HttpSource && sourceQName != null && delegate != null) {
        xLogD("Delegating source: %s -> %s!", sourceQName, delegate.newSourceClass.qualifiedName)
        val enhancedSource = EnhancedHttpSource(
            source,
            // Plain Java reflection: R8 may strip the Kotlin metadata that primaryConstructor would need.
            delegate.newSourceClass.java.getConstructor(HttpSource::class.java, Context::class.java)
                .newInstance(source, context),
        )

        currentDelegatedSources[enhancedSource.originalSource.id] = DelegatedSource(
            enhancedSource.originalSource.name,
            enhancedSource.originalSource.id,
            sourceQName,
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
