package tachiyomi.core.common.storage

import android.content.Context
import android.os.ParcelFileDescriptor
import com.hippo.unifile.UniFile

/** The file extension, or null for directories. */
public val UniFile.extension: String?
    get() = name?.substringAfterLast('.')

/** The name without its extension. */
public val UniFile.nameWithoutExtension: String?
    get() = name?.substringBeforeLast('.')

/** A path suitable for showing to the user. */
public val UniFile.displayablePath: String
    get() = filePath ?: uri.toString()

/** A file descriptor for this file in [mode]. */
public fun UniFile.openFileDescriptor(context: Context, mode: String): ParcelFileDescriptor =
    context.contentResolver.openFileDescriptor(uri, mode) ?: error("Failed to open file descriptor: $displayablePath")
