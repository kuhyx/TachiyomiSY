package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass

/** A context whose connectivity and wifi managers are the given mocks. */
internal fun networkContext(connectivity: ConnectivityManager, wifi: WifiManager): Context {
    val context = mockk<Context>()
    every { context.getSystemService(ConnectivityManager::class.java) } returns connectivity
    every { context.getSystemService(WifiManager::class.java) } returns wifi
    return context
}

/**
 * A connectivity manager reporting [capabilities] for its active network (absent when null) and a legacy
 * network info that is [connected] (absent when null). The legacy info is stubbed dynamically because
 * `NetworkInfo` is deprecated and naming it would fail the build.
 */
internal fun connectivityManager(
    capabilities: NetworkCapabilities?,
    hasActiveNetwork: Boolean = true,
    connected: Boolean? = true,
): ConnectivityManager {
    val manager = mockk<ConnectivityManager>()
    every { manager.activeNetwork } returns mockk<Network>().takeIf { hasActiveNetwork }
    every { manager.getNetworkCapabilities(any()) } returns capabilities
    every { manager.invokeNoArgs("getActiveNetworkInfo") } returns connected?.let(::legacyNetworkInfo)
    return manager
}

/** Network capabilities that hold [transports] and, when [internet], the internet/validated capabilities. */
internal fun networkCapabilities(vararg transports: Int, internet: Boolean = false): NetworkCapabilities {
    val capabilities = mockk<NetworkCapabilities>()
    every { capabilities.hasTransport(any()) } answers { firstArg<Int>() in transports }
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns internet
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns internet
    return capabilities
}

/** A wifi manager whose radio is [enabled], reporting [bssid] on the deprecated connection info. */
internal fun wifiManager(enabled: Boolean, bssid: String? = null): WifiManager {
    val manager = mockk<WifiManager>()
    every { manager.isWifiEnabled } returns enabled
    val info = mockkClass(Class.forName("android.net.wifi.WifiInfo").kotlin)
    every { info.invokeNoArgs("getBSSID") } returns bssid
    every { manager.invokeNoArgs("getConnectionInfo") } returns info
    return manager
}

private fun legacyNetworkInfo(connected: Boolean): Any {
    val info = mockkClass(Class.forName("android.net.NetworkInfo").kotlin)
    every { info.invokeNoArgs("isConnected") } returns connected
    return info
}
