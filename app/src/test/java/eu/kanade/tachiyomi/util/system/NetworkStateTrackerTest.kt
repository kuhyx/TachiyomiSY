package eu.kanade.tachiyomi.util.system

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class NetworkStateTrackerTest {

    @Test
    fun stateWithoutCapabilities() {
        val context = networkContext(connectivityManager(null), wifiManager(enabled = false))
        context.activeNetworkState() shouldBe NetworkState(isConnected = true, isValidated = false, isWifi = false)
    }

    @Test
    fun stateWithAValidatedWifiNetwork() {
        val capabilities = networkCapabilities(NetworkCapabilities.TRANSPORT_WIFI, internet = true)
        val context = networkContext(connectivityManager(capabilities), wifiManager(enabled = true))
        context.activeNetworkState() shouldBe NetworkState(isConnected = true, isValidated = true, isWifi = true)
    }

    @Test
    fun wifiOffBeatsAWifiTransport() {
        val capabilities = networkCapabilities(NetworkCapabilities.TRANSPORT_WIFI, internet = true)
        val context = networkContext(connectivityManager(capabilities), wifiManager(enabled = false))
        context.activeNetworkState().isWifi shouldBe false
    }

    @Test
    fun disconnectedOrAbsentNetwork() {
        val disconnected = networkContext(connectivityManager(null, connected = false), wifiManager(enabled = false))
        disconnected.activeNetworkState().isConnected shouldBe false
        val absent = networkContext(connectivityManager(null, connected = null), wifiManager(enabled = false))
        absent.activeNetworkState().isConnected shouldBe false
    }

    @Test
    fun wifiOnWithoutCapabilities() {
        val wifiOn = networkContext(connectivityManager(null), wifiManager(enabled = true))
        wifiOn.activeNetworkState().isWifi shouldBe false
    }

    @Test
    fun theFlowEmitsOnEveryCallback() = runTest {
        val connectivity = connectivityManager(networkCapabilities())
        val callback = slot<ConnectivityManager.NetworkCallback>()
        every { connectivity.registerDefaultNetworkCallback(capture(callback)) } answers {
            callback.captured.onLost(mockk())
        }
        every { connectivity.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } returns Unit
        val context = networkContext(connectivity, wifiManager(enabled = false))
        context.networkStateFlow().first() shouldBe context.activeNetworkState()
        verify(exactly = 1) { connectivity.unregisterNetworkCallback(callback.captured) }
    }

    @Test
    fun capabilityChangesAlsoEmit() = runTest {
        val connectivity = connectivityManager(networkCapabilities())
        val callback = slot<ConnectivityManager.NetworkCallback>()
        every { connectivity.registerDefaultNetworkCallback(capture(callback)) } answers {
            callback.captured.onCapabilitiesChanged(mockk<Network>(), networkCapabilities())
        }
        every { connectivity.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } returns Unit
        val context = networkContext(connectivity, wifiManager(enabled = false))
        context.networkStateFlow().first().isConnected shouldBe true
    }
}
