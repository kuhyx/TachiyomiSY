package eu.kanade.tachiyomi.util.system

import android.net.NetworkCapabilities
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * The pre-Android-8.1 side of the network helpers: on a plain JVM `Build.VERSION.SDK_INT` is 0, which is
 * the only way to reach the legacy transport ceiling and the legacy wifi check.
 */
internal class NetworkExtensionsTest {

    @Test
    fun systemServicesComeFromThe() {
        val connectivity = connectivityManager(null)
        val wifi = wifiManager(enabled = false)
        val context = networkContext(connectivity, wifi)
        context.connectivityManager shouldBe connectivity
        context.wifiManager shouldBe wifi
    }

    @Test
    fun onlineNeedsATransportBelowThe() {
        val wifi = wifiManager(enabled = false)
        networkContext(connectivityManager(networkCapabilities(NetworkCapabilities.TRANSPORT_WIFI)), wifi)
            .isOnline() shouldBe true
        networkContext(connectivityManager(networkCapabilities(NetworkCapabilities.TRANSPORT_LOWPAN)), wifi)
            .isOnline() shouldBe false
        networkContext(connectivityManager(networkCapabilities()), wifi).isOnline() shouldBe false
    }

    @Test
    fun offlineWithoutANetworkOr() {
        val wifi = wifiManager(enabled = false)
        networkContext(connectivityManager(null, hasActiveNetwork = false), wifi).isOnline() shouldBe false
        networkContext(connectivityManager(null), wifi).isOnline() shouldBe false
    }

    @Test
    fun legacyWifiChecksTheBssid() {
        val connectivity = connectivityManager(null)
        networkContext(connectivity, wifiManager(enabled = false)).isConnectedToWifi() shouldBe false
        networkContext(connectivity, wifiManager(enabled = true)).isConnectedToWifi() shouldBe false
        networkContext(connectivity, wifiManager(enabled = true, bssid = "ab:cd")).isConnectedToWifi() shouldBe true
    }
}
