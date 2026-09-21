package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.getSystemService

internal val Context.connectivityManager: ConnectivityManager
    get() = getSystemService()!!

internal val Context.wifiManager: WifiManager
    get() = getSystemService()!!

internal fun Context.isOnline(): Boolean {
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    val maxTransport = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        NetworkCapabilities.TRANSPORT_LOWPAN
    } else {
        NetworkCapabilities.TRANSPORT_WIFI_AWARE
    }
    return (NetworkCapabilities.TRANSPORT_CELLULAR..maxTransport).any(networkCapabilities::hasTransport)
}

/**
 * Returns true if device is connected to Wifi.
 */
internal fun Context.isConnectedToWifi(): Boolean {
    if (!wifiManager.isWifiEnabled) return false

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val networkCapabilities = connectivityManager.activeNetwork?.let(connectivityManager::getNetworkCapabilities)
        networkCapabilities != null &&
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        wifiManager.connectionInfo.bssid != null
    }
}
