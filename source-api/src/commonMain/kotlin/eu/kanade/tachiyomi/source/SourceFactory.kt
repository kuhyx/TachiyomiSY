package eu.kanade.tachiyomi.source

/**
 * A factory for creating sources at runtime.
 */
public interface SourceFactory {
    /**
     * Create a new copy of the sources
     * @return The created sources
     */
    public fun createSources(): List<Source>
}
