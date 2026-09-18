package tachiyomi.domain.serialization

import java.io.ObjectStreamException
import java.io.Serializable

/**
 * A model that crosses `java.io.Serializable` boundaries (screen arguments,
 * saved state) as a stand-in of its own choosing rather than field by field,
 * so the serialized form survives constructor changes. The JVM finds
 * [writeReplace] through this base class; subclasses only provide the proxy.
 */
public abstract class ProxiedJavaSerializable : Serializable {

    /** The object written in place of this one; it resolves itself back on read. */
    protected abstract fun writeReplacement(): Serializable

    /** Java serialization hook: substitutes [writeReplacement]. */
    @Throws(ObjectStreamException::class)
    protected fun writeReplace(): Any = writeReplacement()

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
