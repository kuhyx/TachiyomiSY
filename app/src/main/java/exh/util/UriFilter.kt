package exh.util

import android.net.Uri

/**
 * Uri filter
. */
internal interface UriFilter {
    fun addToUri(builder: Uri.Builder)
}
