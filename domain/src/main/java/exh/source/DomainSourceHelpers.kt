package exh.source

import eu.kanade.tachiyomi.source.Source
import tachiyomi.domain.manga.model.Manga

// Used to speed up isLewdSource
/** Sorted ids of the sources delegated to a built-in metadata source; set once sources are loaded. */
public var metadataDelegatedSourceIds: List<Long> = emptyList()

/** Sorted ids of the sources delegated to the nhentai implementation; set once sources are loaded. */
public var nHentaiSourceIds: List<Long> = emptyList()

/** Sorted ids of the sources delegated to the LANraragi implementation; set once sources are loaded. */
public var lanraragiSourceIds: List<Long> = emptyList()

/** Sorted ids of the sources delegated to the MangaDex implementation; set once sources are loaded. */
public var mangaDexSourceIds: List<Long> = emptyList()

/** Ids of the sources the library update skips (the EH sites, Pururin and, once loaded, nhentai). */
public var LIBRARY_UPDATE_EXCLUDED_SOURCES: List<Long> = listOf(
    EH_SOURCE_ID,
    EXH_SOURCE_ID,
    PURURIN_SOURCE_ID,
)

// Ids reserved for the built-in metadata sources.
private const val FIRST_METADATA_SOURCE_ID = 6900L
private const val LAST_METADATA_SOURCE_ID = 6999L
private val METADATA_SOURCE_IDS = FIRST_METADATA_SOURCE_ID..LAST_METADATA_SOURCE_ID

// This method MUST be fast!

/** Whether source id [source] is a built-in metadata source or delegated to one. */
public fun isMetadataSource(source: Long): Boolean = source in METADATA_SOURCE_IDS ||
    metadataDelegatedSourceIds.binarySearch(source) >= 0

/** Whether this is the E-Hentai or ExHentai source. */
public fun Source.isEhBasedSource(): Boolean = id == EH_SOURCE_ID || id == EXH_SOURCE_ID

/** Whether this source is delegated to the MangaDex implementation. */
public fun Source.isMdBasedSource(): Boolean = id in mangaDexSourceIds

/** Whether this manga comes from E-Hentai or ExHentai. */
public fun Manga.isEhBasedManga(): Boolean = source == EH_SOURCE_ID || source == EXH_SOURCE_ID
