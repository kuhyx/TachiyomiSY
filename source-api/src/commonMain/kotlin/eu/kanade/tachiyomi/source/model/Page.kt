package eu.kanade.tachiyomi.source.model

import android.net.Uri
import eu.kanade.tachiyomi.network.ProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public open class Page(
    public val index: Int,
    /* SY --> */
    public var /* SY <-- */ url: String = "",
    public var imageUrl: String? = null,
    @Transient public var uri: Uri? = null, // Deprecated but can't be deleted due to extensions
) : ProgressListener {

    public val number: Int
        get() = index + 1

    @Transient
    private val _statusFlow = MutableStateFlow<State>(State.Queue)

    @Transient
    public val statusFlow: StateFlow<State> = _statusFlow.asStateFlow()
    public var status: State
        get() = _statusFlow.value
        set(value) {
            _statusFlow.value = value
        }

    @Transient
    private val _progressFlow = MutableStateFlow(0)

    @Transient
    public val progressFlow: StateFlow<Int> = _progressFlow.asStateFlow()
    public var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progress = if (contentLength > 0) {
            (100 * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }

    public sealed interface State {
        public data object Queue : State
        public data object LoadPage : State
        public data object DownloadImage : State
        public data object Ready : State
        public data class Error(val error: Throwable) : State
    }
}
