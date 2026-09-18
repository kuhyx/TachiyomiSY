package eu.kanade.tachiyomi.util.storage

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ByteArrayOutputStreamPasswordTest {
    @Test
    fun clearOverwritesWithHashes() {
        val stream = ByteArrayOutputStreamPassword()
        stream.write("secret".toByteArray())
        stream.clear()
        stream.toByteArray() shouldBe "######".toByteArray()
    }

    @Test
    fun clearOnEmptyStreamIsHarmless() {
        val stream = ByteArrayOutputStreamPassword()
        stream.clear()
        stream.toByteArray() shouldBe ByteArray(0)
    }
}
