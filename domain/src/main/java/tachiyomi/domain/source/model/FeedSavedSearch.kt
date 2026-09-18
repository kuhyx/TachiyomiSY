package tachiyomi.domain.source.model

/**
 * One entry of the feed tab: a source's latest listing or one of its saved searches, in the
 * global feed or a source's own feed (SY).
 *
 * @property id Row id; unique.
 * @property source Id of the source the entry belongs to.
 * @property savedSearch Id of the [SavedSearch] shown, or null to show the source's latest.
 * @property global Whether the entry is in the global feed rather than the source's feed.
 */
public data class FeedSavedSearch(
    // Tag identifier, unique
    val id: Long,

    // Source for the saved search
    val source: Long,

    // If -1 then get latest, if set get the saved search
    val savedSearch: Long?,

    // If the feed is a global or source specific feed
    val global: Boolean,
)
