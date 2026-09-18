package tachiyomi.domain.storage.service

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn

/**
 * The app's folders under the user-chosen storage directory. Follows the
 * [StoragePreferences.baseStorageDirectory] preference: when it changes the
 * subfolders are created in the new location and [changes] emits.
 */
public class StorageManager(
    private val context: Context,
    storagePreferences: StoragePreferences,
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private var baseDir: UniFile? = getBaseDir(storagePreferences.baseStorageDirectory.get())

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)

    /** Emits after the base directory moved and its subfolders were created; replays the last event. */
    public val changes: SharedFlow<Unit> = _changes.receiveAsFlow()
        .shareIn(scope, SharingStarted.Lazily, 1)

    init {
        storagePreferences.baseStorageDirectory.changes()
            .drop(1)
            .distinctUntilChanged()
            .onEach { uri ->
                baseDir = getBaseDir(uri)
                baseDir?.let { parent ->
                    parent.createDirectory(AUTOMATIC_BACKUPS_PATH)
                    parent.createDirectory(LOCAL_SOURCE_PATH)
                    parent.createDirectory(DOWNLOADS_PATH).also {
                        DiskUtil.createNoMediaFile(it, context)
                    }
                }
                _changes.send(Unit)
            }
            .launchIn(scope)
    }

    private fun getBaseDir(uri: String): UniFile? {
        return UniFile.fromUri(context, uri.toUri())
            .takeIf { it?.exists() == true }
    }

    /** The `autobackup` folder, created on demand; null while the base directory does not exist. */
    public fun getAutomaticBackupsDirectory(): UniFile? = baseDir?.createDirectory(AUTOMATIC_BACKUPS_PATH)

    /** The `downloads` folder, created on demand; null while the base directory does not exist. */
    public fun getDownloadsDirectory(): UniFile? = baseDir?.createDirectory(DOWNLOADS_PATH)

    /** The `local` folder of the local source, created on demand; null while the base directory does not exist. */
    public fun getLocalSourceDirectory(): UniFile? = baseDir?.createDirectory(LOCAL_SOURCE_PATH)

    // SY -->

    /** The `logs` folder, created on demand; null while the base directory does not exist. */
    public fun getLogsDirectory(): UniFile? = baseDir?.createDirectory(LOGS_PATH)
    // SY <--
}

private const val AUTOMATIC_BACKUPS_PATH = "autobackup"
private const val DOWNLOADS_PATH = "downloads"
private const val LOCAL_SOURCE_PATH = "local"

// SY -->
private const val LOGS_PATH = "logs"
// SY <--
