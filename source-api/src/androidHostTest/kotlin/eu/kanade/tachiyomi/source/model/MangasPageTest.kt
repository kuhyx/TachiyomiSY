package eu.kanade.tachiyomi.source.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredFunctions

internal class MangasPageTest {
    private val manga = SManga("/u", "Title")
    private val page = MangasPage(listOf(manga), true)

    // The deprecated members are reached through reflection: a direct call is a warning, and warnings are errors.
    private fun deprecated(name: String): KFunction<*> = MangasPage::class.declaredFunctions.first { it.name == name }

    private fun KFunction<*>.parameter(name: String): KParameter = parameters.first { it.name == name }

    private fun KFunction<*>.instance(): KParameter = parameters.first { it.kind == KParameter.Kind.INSTANCE }

    @Test
    fun sameInstanceIsEqual() {
        val same: Any = page
        (page == same) shouldBe true
    }

    @Test
    fun otherTypeIsNotEqual() {
        val other: Any = "page"
        (page == other) shouldBe false
    }

    @Test
    fun differentMangasAreNotEqual() {
        (page == MangasPage(emptyList(), true)) shouldBe false
    }

    @Test
    fun differentNextPageIsNotEqual() {
        (page == MangasPage(listOf(manga), false)) shouldBe false
    }

    @Test
    fun equalFieldsAreEqual() {
        page shouldBe MangasPage(listOf(manga), true)
    }

    @Test
    fun hashCodeCombinesFields() {
        page.hashCode() shouldBe 31 * listOf(manga).hashCode() + true.hashCode()
    }

    @Test
    fun toStringListsFields() {
        page.toString() shouldBe "MangasPage(mangas=${listOf(manga)}, hasNextPage=true)"
    }

    @Test
    fun deprecatedComponents() {
        deprecated("component1").call(page) shouldBe listOf(manga)
        deprecated("component2").call(page) shouldBe true
    }

    @Test
    fun deprecatedCopyKeepsDefaults() {
        val copy = deprecated("copy")
        copy.callBy(mapOf(copy.instance() to page)) shouldBe page
        val withoutMangas = copy.callBy(mapOf(copy.instance() to page, copy.parameter("mangas") to emptyList<SManga>()))
        withoutMangas shouldBe MangasPage(emptyList(), true)
        val withoutNextPage = copy.callBy(mapOf(copy.instance() to page, copy.parameter("hasNextPage") to false))
        withoutNextPage shouldBe MangasPage(listOf(manga), false)
    }

    @Test
    fun deprecatedCopyReplacesFields() {
        val copy = deprecated("copy")
        val arguments = mapOf(
            copy.instance() to page,
            copy.parameter("mangas") to emptyList<SManga>(),
            copy.parameter("hasNextPage") to false,
        )
        copy.callBy(arguments) shouldBe MangasPage(emptyList(), false)
    }
}
