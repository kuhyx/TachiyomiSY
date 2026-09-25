package mihon.core.migration.migrations

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test
import kotlin.reflect.full.createType

/**
 * The private serializable configs of [MergedMangaRewriteMigration]: their generated data-class members and
 * serializers, reached through reflection because the classes are private to the migration.
 */
internal class MergedMangaRewriteModelsTest {

    private val migration = MergedMangaRewriteMigration::class.java.name

    @Test
    fun urlConfigIsAValue() {
        val cls = Class.forName("$migration\$UrlConfig")
        val a = newInstance(cls, 1L, "u", "m")
        val b = newInstance(cls, 1L, "u", "m")
        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
        a shouldNotBe newInstance(cls, 2L, "u", "m")
        a.toString() shouldContain "UrlConfig(source=1, url=u, mangaUrl=m)"
        call(cls, a, "copy", 3L, "x", "y").toString() shouldContain "source=3"
        call(cls, a, "component1") shouldBe 1L
        call(cls, a, "component2") shouldBe "u"
        call(cls, a, "component3") shouldBe "m"
        encode(cls, a) shouldBe """{"s":1,"u":"u","m":"m"}"""
    }

    @Test
    fun mangaSourceIsAValue() {
        val cls = Class.forName("$migration\$MangaSource")
        val a = newInstance(cls, 1L, "u")
        a shouldBe newInstance(cls, 1L, "u")
        a shouldNotBe newInstance(cls, 1L, "v")
        a.hashCode() shouldBe newInstance(cls, 1L, "u").hashCode()
        a.toString() shouldContain "MangaSource(source=1, url=u)"
        call(cls, a, "copy", 2L, "w").toString() shouldContain "url=w"
        call(cls, a, "component1") shouldBe 1L
        call(cls, a, "component2") shouldBe "u"
        encode(cls, a) shouldBe """{"s":1,"u":"u"}"""
    }

    @Test
    fun mangaConfigIsAValue() {
        val sourceCls = Class.forName("$migration\$MangaSource")
        val child = newInstance(sourceCls, 1L, "u")
        val cls = Class.forName("$migration\$MangaConfig")
        val a = newInstance(cls, listOf(child))
        a shouldBe newInstance(cls, listOf(child))
        a shouldNotBe newInstance(cls, emptyList<Any>())
        a.hashCode() shouldBe newInstance(cls, listOf(child)).hashCode()
        a.toString() shouldContain "MangaConfig(children=[MangaSource(source=1, url=u)])"
        call(cls, a, "copy", emptyList<Any>()).toString() shouldContain "children=[]"
        call(cls, a, "component1") shouldBe listOf(child)
        encode(cls, a) shouldBe """{"c":[{"s":1,"u":"u"}]}"""
    }

    private fun newInstance(cls: Class<*>, vararg args: Any): Any {
        val constructor = cls.declaredConstructors.first { it.parameterCount == args.size }
        constructor.isAccessible = true
        return constructor.newInstance(*args)
    }

    private fun call(cls: Class<*>, receiver: Any, name: String, vararg args: Any): Any? {
        val method = cls.declaredMethods.first { it.name == name && it.parameterCount == args.size }
        method.isAccessible = true
        return method.invoke(receiver, *args)
    }

    private fun encode(cls: Class<*>, value: Any): String =
        Json.encodeToString(serializer(cls.kotlin.createType()), value)
}
