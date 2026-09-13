package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.RaisedSearchMetadata

/** A source that can list the manga the logged-in user follows. */
public interface FollowsSource : Source {
    /** One page of the user's follows. */
    public suspend fun fetchFollows(page: Int): MangasPage

    /** Every follow of the user, each with its parsed metadata. */
    public suspend fun fetchAllFollows(): List<Pair<SManga, RaisedSearchMetadata>>
}
