package eu.kanade.tachiyomi.source

/**
 * A factory for creating sources at runtime.
 */
public interface SourceFactory {
    /**
     * Creates a new copy of the sources.
     *
     * @return the created sources.
     */
    public fun createSources(): List<Source>
}
