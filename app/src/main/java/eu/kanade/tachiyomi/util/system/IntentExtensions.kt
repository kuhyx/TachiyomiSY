package eu.kanade.tachiyomi.util.system

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.IntentCompat
import mihon.core.common.InlinedOnly
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.io.Serializable

internal fun Uri.toShareIntent(context: Context, type: String = "image/*", message: String? = null): Intent {
    val uri = this

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        when (uri.scheme) {
            "http", "https" -> {
                putExtra(Intent.EXTRA_TEXT, uri.toString())
            }
            "content" -> {
                message?.let { putExtra(Intent.EXTRA_TEXT, it) }
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        }
        clipData = ClipData.newRawUri(null, uri)
        setType(type)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    return Intent.createChooser(shareIntent, context.stringResource(MR.strings.action_share)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}

// The reified overloads carry distinct JVM names: Kover's annotation filter keys methods by
// name, and the non-reified workers below must stay measured.

/** [IntentCompat.getParcelableExtra] for [T]. */
@InlinedOnly
@JvmName("getParcelableExtraCompatReified")
internal inline fun <reified T> Intent.getParcelableExtraCompat(name: String): T? =
    getParcelableExtraCompat(name, T::class.java)

/** [IntentCompat.getParcelableExtra] for [clazz]. */
internal fun <T> Intent.getParcelableExtraCompat(name: String, clazz: Class<T>): T? =
    IntentCompat.getParcelableExtra(this, name, clazz)

/** The serializable extra [name] as [T], through the API the running platform offers. */
@InlinedOnly
@JvmName("getSerializableExtraCompatReified")
internal inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(name: String): T? =
    getSerializableExtraCompat(name, T::class.java)

/** The serializable extra [name] as [clazz], through the API the running platform offers. */
internal fun <T : Serializable> Intent.getSerializableExtraCompat(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(name, clazz)
    } else {
        @Suppress("DEPRECATION")
        clazz.cast(getSerializableExtra(name))
    }
}
