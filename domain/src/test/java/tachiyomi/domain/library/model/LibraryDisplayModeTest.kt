package tachiyomi.domain.library.model

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.net.URL
import java.net.URLClassLoader

internal class LibraryDisplayModeTest {

    @Test
    fun serializeNamesEveryMode() {
        LibraryDisplayMode.CompactGrid.serialize() shouldBe "COMPACT_GRID"
        LibraryDisplayMode.ComfortableGrid.serialize() shouldBe "COMFORTABLE_GRID"
        LibraryDisplayMode.CoverOnlyGrid.serialize() shouldBe "COVER_ONLY_GRID"
        LibraryDisplayMode.List.serialize() shouldBe "LIST"
    }

    @Test
    fun deserializeParsesEveryName() {
        LibraryDisplayMode.deserialize("COMPACT_GRID") shouldBe LibraryDisplayMode.CompactGrid
        LibraryDisplayMode.deserialize("COMFORTABLE_GRID") shouldBe LibraryDisplayMode.ComfortableGrid
        LibraryDisplayMode.deserialize("COVER_ONLY_GRID") shouldBe LibraryDisplayMode.CoverOnlyGrid
        LibraryDisplayMode.deserialize("LIST") shouldBe LibraryDisplayMode.List
    }

    @Test
    fun deserializeUnknownIsDefault() {
        LibraryDisplayMode.deserialize("") shouldBe LibraryDisplayMode.default
        LibraryDisplayMode.deserialize("list") shouldBe LibraryDisplayMode.CompactGrid
    }

    @Test
    fun roundTripsEveryMode() {
        LibraryDisplayMode.values.forEach { mode ->
            LibraryDisplayMode.deserialize(mode.serialize()) shouldBe mode
        }
    }

    @Test
    fun serializerObjectDelegates() {
        LibraryDisplayMode.Serializer.serialize(LibraryDisplayMode.List) shouldBe "LIST"
        LibraryDisplayMode.Serializer.deserialize("LIST") shouldBe LibraryDisplayMode.List
        LibraryDisplayMode.Serializer.deserialize("nope") shouldBe LibraryDisplayMode.CompactGrid
    }

    @Test
    fun valuesInMenuOrder() {
        LibraryDisplayMode.values.toList() shouldContainExactly listOf(
            LibraryDisplayMode.CompactGrid,
            LibraryDisplayMode.ComfortableGrid,
            LibraryDisplayMode.List,
            LibraryDisplayMode.CoverOnlyGrid,
        )
        LibraryDisplayMode.default shouldBe LibraryDisplayMode.CompactGrid
    }

    // A fresh class loader that meets CompactGrid before the interface: the order that left
    // `default` null (67 Robolectric tests failed once the suite ran in two forks).
    @Test
    fun defaultSurvivesAnyLoadOrder() {
        val urls = arrayOf(locationOf(LibraryDisplayMode::class.java), locationOf(Unit::class.java))
        URLClassLoader(urls, null).use { loader ->
            Class.forName("${LibraryDisplayMode::class.java.name}\$CompactGrid", true, loader)
            val companion = Class.forName(LibraryDisplayMode::class.java.name, true, loader)
                .getField("Companion")
                .get(null)
            companion.javaClass.getMethod("getDefault").invoke(companion) shouldNotBe null
        }
    }

    @Test
    fun modesAreDistinctDataObjects() {
        LibraryDisplayMode.values.map { it.toString() }.toSet().size shouldBe 4
        LibraryDisplayMode.values.map { it.hashCode() }.toSet().size shouldBe 4
        LibraryDisplayMode.List.toString() shouldBe "List"
        LibraryDisplayMode.List shouldNotBe LibraryDisplayMode.CompactGrid
    }
}

private fun locationOf(type: Class<*>): URL = checkNotNull(type.protectionDomain?.codeSource?.location)
