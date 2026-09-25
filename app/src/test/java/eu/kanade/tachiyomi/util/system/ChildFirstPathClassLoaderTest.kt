package eu.kanade.tachiyomi.util.system

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.InvocationTargetException
import java.net.URL

@RunWith(RobolectricTestRunner::class)
internal class ChildFirstPathClassLoaderTest {

    private val loader = ChildFirstPathClassLoader(
        dexPath = "",
        librarySearchPath = null,
        parent = javaClass.classLoader!!,
    )

    // The override is protected and the class is final, so the tests call the app class's own declared method.
    private fun load(name: String, resolve: Boolean): Class<*> {
        val method = ChildFirstPathClassLoader::class.java
            .getDeclaredMethod("loadClass", String::class.java, Boolean::class.javaPrimitiveType)
        method.isAccessible = true
        return method.invoke(loader, name, resolve) as Class<*>
    }

    @Test
    fun theSystemLoaderWinsForItsOwn() {
        load("java.lang.String", false) shouldBe String::class.java
        load("java.lang.String", true) shouldBe String::class.java
        loader.loadClass("java.lang.Thread") shouldBe Thread::class.java
    }

    @Test
    fun theParentServesEverythingElse() {
        val name = ChildFirstPathClassLoaderTest::class.java.name
        load(name, false).name shouldBe name
        val failure = shouldThrow<InvocationTargetException> { load("com.example.absent.Type", false) }
        failure.cause!!::class shouldBe ClassNotFoundException::class
    }

    @Test
    fun theParentCanStillDefineAClass() {
        val fake = ChildFirstPathClassLoader("", null, FakeParent(javaClass.classLoader!!))
        val method = ChildFirstPathClassLoader::class.java
            .getDeclaredMethod("loadClass", String::class.java, Boolean::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(fake, FakeParent.FAKE_CLASS, false) shouldBe String::class.java
    }

    @Test
    fun anUnreadableResourceStreamIs() {
        val fake = ChildFirstPathClassLoader("", null, FakeParent(javaClass.classLoader!!))
        fake.getResource(FakeParent.FAKE_RESOURCE).shouldNotBeNull()
        fake.getResourceAsStream(FakeParent.FAKE_RESOURCE).shouldBeNull()
    }

    @Test
    fun resourcesComeFromTheFirst() {
        loader.getResource("robolectric.properties").shouldNotBeNull()
        loader.getResource("does/not/exist").shouldBeNull()
        loader.getResourceAsStream("robolectric.properties").shouldNotBeNull()
        loader.getResourceAsStream("does/not/exist").shouldBeNull()
    }

    @Test
    fun resourcesAreConcatenatedAcross() {
        val urls = loader.getResources("robolectric.properties")
        urls.hasMoreElements() shouldBe true
        var count = 0
        while (urls.hasMoreElements()) {
            urls.nextElement()
            count++
        }
        (count >= 1) shouldBe true
        loader.getResources("does/not/exist").hasMoreElements() shouldBe false
    }
}

/** A parent loader that owns one class and one resource the system loader knows nothing about. */
private class FakeParent(parent: ClassLoader) : ClassLoader(parent) {
    override fun loadClass(name: String?, resolve: Boolean): Class<*> =
        if (name == FAKE_CLASS) String::class.java else super.loadClass(name, resolve)

    override fun getResource(name: String?): URL? =
        if (name == FAKE_RESOURCE) URL("file:/nonexistent/fake.txt") else super.getResource(name)

    companion object {
        const val FAKE_CLASS = "com.example.OnlyInTheParent"
        const val FAKE_RESOURCE = "only/in/the/parent.txt"
    }
}
