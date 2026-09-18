package tachiyomi.domain.serialization

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/** A model that is written as [Proxy] and rebuilt from it; [Proxied.isRebuilt] shows the proxy path was taken. */
internal data class Proxied(val label: String, val isRebuilt: Boolean = false) : ProxiedJavaSerializable() {
    override fun writeReplacement(): Serializable = Proxy(label)
}

/** The stand-in written to the stream; it resolves back to a [Proxied] marked as rebuilt. */
internal open class Proxy(private val label: String) : Serializable {
    protected fun readResolve(): Any = Proxied(label = label, isRebuilt = true)

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal class ProxiedJavaSerializableTest {

    private fun roundTrip(value: Any): Any {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        return ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
    }

    @Test
    fun writesProxyInsteadOfItself() {
        val restored = roundTrip(Proxied("cover")).shouldBeInstanceOf<Proxied>()

        restored.label shouldBe "cover"
        restored.isRebuilt shouldBe true
        restored shouldBe Proxied(label = "cover", isRebuilt = true)
    }

    @Test
    fun proxyRoundTripsInContainers() {
        val restored = roundTrip(arrayListOf(Proxied("a"), Proxied("b")))

        restored shouldBe listOf(Proxied(label = "a", isRebuilt = true), Proxied(label = "b", isRebuilt = true))
    }
}
