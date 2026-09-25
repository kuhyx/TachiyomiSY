package eu.kanade.tachiyomi.util.system

import android.net.NetworkCapabilities
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The modern side of the network helpers, where Robolectric reports a current SDK level. */
@RunWith(RobolectricTestRunner::class)
internal class NetworkExtensionsSdkTest {

    @Test
    fun theTransportCeilingIsLowpan() {
        val wifi = wifiManager(enabled = false)
        networkContext(connectivityManager(networkCapabilities(NetworkCapabilities.TRANSPORT_LOWPAN)), wifi)
            .isOnline() shouldBe true
    }

    @Test
    fun wifiNeedsAValidatedWifi() {
        networkContext(connectivityManager(networkCapabilities()), wifiManager(enabled = false))
            .isConnectedToWifi() shouldBe false
        val onlyWifi = networkCapabilities(NetworkCapabilities.TRANSPORT_WIFI)
        networkContext(connectivityManager(onlyWifi), wifiManager(enabled = true)).isConnectedToWifi() shouldBe false
        val validated = networkCapabilities(NetworkCapabilities.TRANSPORT_WIFI, internet = true)
        networkContext(connectivityManager(validated), wifiManager(enabled = true)).isConnectedToWifi() shouldBe true
        networkContext(connectivityManager(null), wifiManager(enabled = true)).isConnectedToWifi() shouldBe false
        val noInternet = networkCapabilities(NetworkCapabilities.TRANSPORT_WIFI)
        networkContext(connectivityManager(noInternet), wifiManager(enabled = true)).isConnectedToWifi() shouldBe false
        val wifiless = networkCapabilities(NetworkCapabilities.TRANSPORT_CELLULAR, internet = true)
        networkContext(connectivityManager(wifiless), wifiManager(enabled = true)).isConnectedToWifi() shouldBe false
        val noNetwork = connectivityManager(validated, hasActiveNetwork = false)
        networkContext(noNetwork, wifiManager(enabled = true)).isConnectedToWifi() shouldBe false
    }
}
