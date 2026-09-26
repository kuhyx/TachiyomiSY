package eu.kanade.tachiyomi.data.download

import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/** Package Robolectric must instrument so the shadows below replace libarchive's JNI. */
internal const val LIBARCHIVE_PACKAGE: String = "me.zhanghai.android.libarchive"

/**
 * libarchive's JNI library is built for Android ABIs only. Under instrumentation its native methods
 * become stubs answering defaults; these shadows only replace the static initialisers that would
 * load the library. (Named by string: libarchive is core's implementation dependency.)
 */
@Implements(className = "me.zhanghai.android.libarchive.Archive")
internal object ShadowArchive {
    @JvmStatic
    @Implementation
    fun __staticInitializer__() {
        // The JNI library is never loaded.
    }
}

/** See [ShadowArchive]. */
@Implements(className = "me.zhanghai.android.libarchive.ArchiveEntry")
internal object ShadowArchiveEntry {
    @JvmStatic
    @Implementation
    fun __staticInitializer__() {
        // The JNI library is never loaded.
    }
}
