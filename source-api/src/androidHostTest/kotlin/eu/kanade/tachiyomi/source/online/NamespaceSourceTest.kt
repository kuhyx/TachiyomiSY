package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** A source carrying the [NamespaceSource] marker. */
private class NamespacedStubSource : StubSource(), NamespaceSource

/** [NamespaceSource] is a marker: it adds nothing and stays a [Source]. */
internal class NamespaceSourceTest {
    @Test
    fun markerKeepsSourceIdentity() {
        val source: Source = NamespacedStubSource()
        (source is NamespaceSource) shouldBe true
        (StubSource() is NamespaceSource) shouldBe false
        source.name shouldBe "Stub Source"
    }
}
