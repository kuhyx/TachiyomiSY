package eu.kanade.tachiyomi.util.system

import android.os.Build

internal data class NetworkState(
    val isConnected: Boolean,
    val isValidated: Boolean,
    val isWifi: Boolean,
) {
    val isOnline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        isConnected && isValidated
    } else {
        isConnected
    }
}
