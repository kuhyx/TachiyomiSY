package eu.kanade.tachiyomi.util.system

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ClassName
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowDexFile
import java.io.File

private const val DEX_ONLY_CLASS = "com.example.dex.OnlyInTheDex"
private const val DIR_RESOURCE = "only/in/the/dir.txt"

/** What the loader's own path serves: resources from a directory entry, classes from a dex entry. */
@RunWith(RobolectricTestRunner::class)
internal class ChildFirstPathClassLoaderDexTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun aDirectoryServesResources() {
        val dir = folder.newFolder("resources")
        File(dir, DIR_RESOURCE).apply {
            parentFile?.mkdirs()
            writeText("local")
        }
        val loader = ChildFirstPathClassLoader(dir.path, null, javaClass.classLoader!!)
        loader.getResource(DIR_RESOURCE).shouldNotBeNull().readText() shouldBe "local"
        loader.getResources(DIR_RESOURCE).toList().size shouldBe 1
    }

    @After
    fun tearDown() {
        ShadowDexDefineClass.definitions.clear()
    }

    @Test
    @Config(shadows = [ShadowDexDefineClass::class])
    fun theDexWinsOverTheParent() {
        val dex = folder.newFile("classes.dex")
        val loader = ChildFirstPathClassLoader(dex.path, null, javaClass.classLoader!!)
        loader.loadClass(DEX_ONLY_CLASS) shouldBe DexDefinedClass::class.java
        val definition = ShadowDexDefineClass.definitions.single()
        definition.name shouldBe DEX_ONLY_CLASS
        definition.loader shouldBe loader
        definition.dexFile.shouldNotBeNull()
    }
}

/** Stands in for a class that only the dex on the loader's path defines. */
internal class DexDefinedClass

/** One call to the native definition: the class name, its defining loader, the dex cookie and the dex file. */
internal data class DexDefinition(val name: String, val loader: ClassLoader?, val cookie: Any?, val dexFile: Any?)

/**
 * Robolectric's own DexFile shadow, plus class definition: the JVM cannot run ART's native one, so the
 * dex's one class is "defined" here and every other name is not in this dex.
 */
@Implements(className = "dalvik.system.DexFile", isInAndroidSdk = false)
internal class ShadowDexDefineClass : ShadowDexFile() {
    companion object {
        /** Every definition asked of the dex. */
        val definitions: MutableList<DexDefinition> = mutableListOf()

        @JvmStatic
        @Implementation
        fun defineClassNative(
            name: String,
            loader: ClassLoader?,
            cookie: Any?,
            @ClassName("dalvik.system.DexFile") dexFile: Any?,
        ): Class<*>? {
            definitions += DexDefinition(name = name, loader = loader, cookie = cookie, dexFile = dexFile)
            return DexDefinedClass::class.java.takeIf { name == DEX_ONLY_CLASS }
        }
    }
}
