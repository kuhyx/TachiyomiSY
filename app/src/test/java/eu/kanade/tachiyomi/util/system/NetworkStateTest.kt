package eu.kanade.tachiyomi.util.system

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class NetworkStateTest {

    @Test
    fun onlineNeedsConnectedAnd() {
        NetworkState(isConnected = true, isValidated = true, isWifi = false).isOnline shouldBe true
        NetworkState(isConnected = true, isValidated = false, isWifi = true).isOnline shouldBe false
        NetworkState(isConnected = false, isValidated = true, isWifi = true).isOnline shouldBe false
    }

    @Test
    fun isAValue() {
        val state = NetworkState(isConnected = true, isValidated = true, isWifi = true)
        state shouldBe state.copy()
        state.hashCode() shouldBe state.copy().hashCode()
        state.toString() shouldBe "NetworkState(isConnected=true, isValidated=true, isWifi=true)"
        state.component1() shouldBe true
        state.component3() shouldBe true
    }
}
