package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okhttp3.CacheControl
import okhttp3.Response

/** A source that serves page thumbnails ahead of the chapter download. */

public interface PagePreviewSource : Source {

    /** One page of previews for [manga], given its [chapters]. */

    public suspend fun getPagePreviewList(manga: SManga, chapters: List<SChapter>, page: Int): PagePreviewPage

    /** The preview image of [page], honouring [cacheControl]. */

    public suspend fun fetchPreviewImage(page: PagePreviewInfo, cacheControl: CacheControl? = null): Response
}

/**
 * One page of previews and whether more follow.
 *
 * @property page the page number this batch is for.
 * @property pagePreviews the previews.
 * @property hasNextPage whether another batch can be requested.
 * @property pagePreviewPages total number of batches, when known.
 */
@Serializable
public data class PagePreviewPage(
    val page: Int,
    val pagePreviews: List<PagePreviewInfo>,
    val hasNextPage: Boolean,
    val pagePreviewPages: Int?,
)

/**
 * A single preview: its position, image URL and download progress.
 *
 * @property index zero-based position.
 * @property imageUrl the thumbnail URL.
 * @param progressState backing state of [progress].
 */
@Serializable
public data class PagePreviewInfo(
    val index: Int,
    val imageUrl: String,
    @Transient
    private val progressState: MutableStateFlow<Int> = MutableStateFlow(-1),
) : ProgressListener {
    /** Download progress in percent, or -1 when unknown. */
    @Transient
    public val progress: StateFlow<Int> = progressState.asStateFlow()

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progressState.value = if (contentLength > 0) {
            (PERCENT * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }
}

private const val PERCENT: Long = 100
