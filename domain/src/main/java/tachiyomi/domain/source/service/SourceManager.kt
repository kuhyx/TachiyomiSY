package tachiyomi.domain.source.service

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.source.model.StubSource

/** Registry of the loaded sources: installed extensions, built-ins and stubs for missing ones. */
public interface SourceManager {

    /** Whether the first extension scan has completed; lookups before that see an empty registry. */
    public val isInitialized: StateFlow<Boolean>

    /** Every loaded source, re-emitted whenever an extension is installed, updated or removed. */
    public val sources: Flow<List<Source>>

    /** The source with id [sourceKey], or null when no such source is loaded. */
    public fun get(sourceKey: Long): Source?

    /** The source with id [sourceKey], or a [StubSource] for it when it is not loaded. */
    public fun getOrStub(sourceKey: Long): Source

    /** Every loaded source, stubs included. */
    public fun getAll(): List<Source>

    /** The loaded sources that fetch over HTTP. */
    public fun getOnlineSources(): List<HttpSource>

    // SY -->

    /** [getOnlineSources] minus the sources hidden by the blacklist. */
    public fun getVisibleOnlineSources(): List<HttpSource>

    /** [getAll] minus the sources hidden by the blacklist. */
    public fun getVisibleSources(): List<Source>
    // SY <--

    /** The stubs standing in for sources that are not installed. */
    public fun getStubSources(): List<StubSource>
}
