package exh.metadata.metadata.base

/** Metadata that carries the ids of the manga on the tracking services. */
public interface TrackerIdMetadata {
    /** AniList id. */
    public var anilistId: String?

    /** Kitsu id. */
    public var kitsuId: String?

    /** MyAnimeList id. */
    public var myAnimeListId: String?

    /** MangaUpdates id. */
    public var mangaUpdatesId: String?

    /** Anime-Planet id. */
    public var animePlanetId: String?
}
