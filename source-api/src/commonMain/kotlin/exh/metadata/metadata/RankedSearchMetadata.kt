package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable

/** Metadata of a ranked listing entry. */
@Serializable
public class RankedSearchMetadata : RaisedSearchMetadata() {
    /** Position in the ranking. */
    public var rank: Int? = null

    override fun createMangaInfo(manga: SManga): SManga = manga
    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> = emptyList()
}
