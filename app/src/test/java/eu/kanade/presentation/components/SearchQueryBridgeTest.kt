package eu.kanade.presentation.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SearchQueryBridgeTest {

    @Test
    fun staleEchoKeepsNewerKeystrokes() {
        val bridge = SearchQueryBridge("")
        bridge.fieldChanged("C") shouldBe true
        bridge.fieldChanged("Ch") shouldBe true
        bridge.fieldChanged("Cha") shouldBe true
        // The caller trails: each echo arrives after the field has moved on.
        bridge.callerChanged("C") shouldBe false
        bridge.callerChanged("Ch") shouldBe false
        bridge.callerChanged("Cha") shouldBe false
    }

    @Test
    fun unreportedValueOverwritesField() {
        val bridge = SearchQueryBridge("")
        bridge.fieldChanged("abc") shouldBe true
        bridge.callerChanged("") shouldBe true
        // The field then reports the value it was just given; that is not a change to relay.
        bridge.fieldChanged("") shouldBe false
    }

    @Test
    fun overwriteDropsEveryPendingEcho() {
        val bridge = SearchQueryBridge("")
        bridge.fieldChanged("a") shouldBe true
        bridge.fieldChanged("ab") shouldBe true
        bridge.callerChanged("restored") shouldBe true
        bridge.callerChanged("a") shouldBe true
        bridge.callerChanged("ab") shouldBe true
    }

    @Test
    fun repeatedValuesMatchInOrder() {
        val bridge = SearchQueryBridge("")
        bridge.fieldChanged("a") shouldBe true
        bridge.fieldChanged("") shouldBe true
        bridge.fieldChanged("a") shouldBe true
        bridge.callerChanged("a") shouldBe false
        bridge.callerChanged("") shouldBe false
        bridge.callerChanged("a") shouldBe false
    }

    @Test
    fun initialValueIsLeftAlone() {
        val bridge = SearchQueryBridge("seed")
        bridge.callerChanged("seed") shouldBe false
        bridge.fieldChanged("seed") shouldBe false
    }
}
