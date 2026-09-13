package tachiyomi.core.common.storage

import android.content.Context
import android.os.ParcelFileDescriptor
import com.hippo.unifile.UniFile

public val UniFile.extension: String?
    get() = name?.substringAfterLast('.')

public val UniFile.nameWithoutExtension: String?
    get() = name?.substringBeforeLast('.')

public val UniFile.displayablePath: String
    get() = filePath ?: uri.toString()

public fun UniFile.openFileDescriptor(context: Context, mode: String): ParcelFileDescriptor =
    context.contentResolver.openFileDescriptor(uri, mode) ?: error("Failed to open file descriptor: $displayablePath")
