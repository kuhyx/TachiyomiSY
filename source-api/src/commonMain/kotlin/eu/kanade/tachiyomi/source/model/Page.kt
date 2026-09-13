package eu.kanade.tachiyomi.source.model

import android.net.Uri
import eu.kanade.tachiyomi.network.ProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * One page of a chapter: where its image lives and how far its download got.
 *
 * @property index zero-based position in the chapter.
 * @property url the page URL, resolved to [imageUrl] on demand.
 * @property imageUrl the direct image URL once known.
 * @property uri deprecated local file location, kept for extensions.
 */
@Serializable
public open class Page(
    public val index: Int,
    /* SY --> */
    public var /* SY <-- */ url: String = "",
    public var imageUrl: String? = null,
    @Transient public var uri: Uri? = null, // Deprecated but can't be deleted due to extensions
) : ProgressListener {

    /** One-based page number for display. */
    public val number: Int
        get() = index + 1

    @Transient
    private val _statusFlow = MutableStateFlow<State>(State.Queue)

    /** Observable [status]. */
    @Transient
    public val statusFlow: StateFlow<State> = _statusFlow.asStateFlow()

    /** Current loading state. */
    public var status: State
        get() = _statusFlow.value
        set(value) {
            _statusFlow.value = value
        }

    @Transient
    private val _progressFlow = MutableStateFlow(0)

    /** Observable [progress]. */
    @Transient
    public val progressFlow: StateFlow<Int> = _progressFlow.asStateFlow()

    /** Download progress in percent, or -1 when the size is unknown. */
    public var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progress = if (contentLength > 0) {
            (PERCENT * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }

    /** Loading state of a page. */
    public sealed interface State {
        /** Waiting to be loaded. */
        public data object Queue : State

        /** Resolving the image URL. */
        public data object LoadPage : State

        /** Downloading the image. */
        public data object DownloadImage : State

        /** The image is available. */
        public data object Ready : State

        /**
         * Loading failed.
         *
         * @property error the cause.
         */
        public data class Error(val error: Throwable) : State
    }
}

private const val PERCENT: Long = 100
