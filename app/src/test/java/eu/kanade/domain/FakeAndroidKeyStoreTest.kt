package eu.kanade.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.security.KeyStore

internal class FakeAndroidKeyStoreTest {

    @Test
    fun answersTheAndroidKeyStoreType() {
        installFakeAndroidKeyStore()
        installFakeAndroidKeyStore()
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.size() shouldBe 0
        keyStore.containsAlias("x") shouldBe false
        keyStore.getEntry("x", null) shouldBe null
    }
}
