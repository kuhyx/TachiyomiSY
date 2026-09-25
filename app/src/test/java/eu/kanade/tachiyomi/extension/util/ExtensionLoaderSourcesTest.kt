package eu.kanade.tachiyomi.extension.util

import android.app.Application
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SOURCE_CLASS = "eu.kanade.tachiyomi.extension.util.LoadedTestSource"
private const val FACTORY_CLASS = "eu.kanade.tachiyomi.extension.util.LoadedTestFactory"

/**
 * The loader asks the JVM's system class loader first, which answers with the *uninstrumented*
 * copy of a class on the test classpath, so its `Source` is a different type than the sandbox's
 * and every instantiation ends in the "unknown source class type" arm. Every line still runs.
 */
@RunWith(RobolectricTestRunner::class)
internal class ExtensionLoaderSourcesTest {
    private val context: Application = ApplicationProvider.getApplicationContext()

    private fun load(sourceClass: String?, sourceDir: String? = "/data/app/ext.apk"): List<String>? {
        val pkgInfo: PackageInfo = extensionPackage(
            metaData = extensionMetaData(sourceClass = sourceClass),
            sourceDir = sourceDir,
        )
        return ExtensionLoader.loadSources(
            context = context,
            pkgInfo = pkgInfo,
            appInfo = pkgInfo.applicationInfo!!,
            extName = "Ext Name",
        )?.map { it.lang }
    }

    @Test
    fun aSingleSourceClass() {
        load(SOURCE_CLASS).shouldBeNull()
    }

    @Test
    fun aFactoryClass() {
        load(FACTORY_CLASS).shouldBeNull()
    }

    @Test
    fun severalClassesInOneEntry() {
        load(".LoadedTestSource;$FACTORY_CLASS").shouldBeNull()
    }

    @Test
    fun aRelativeNameIsResolved() {
        val pkgInfo = extensionPackage(
            pkgName = "eu.kanade.tachiyomi.extension.util",
            metaData = extensionMetaData(sourceClass = " .LoadedTestSource "),
        )
        ExtensionLoader.loadSources(
            context = context,
            pkgInfo = pkgInfo,
            appInfo = pkgInfo.applicationInfo!!,
            extName = "Ext Name",
        ).shouldBeNull()
    }

    @Test
    fun anUnknownClassType() {
        load("eu.kanade.tachiyomi.extension.util.NotASourceAtAll").shouldBeNull()
    }

    @Test
    fun aMissingClass() {
        load("eu.kanade.tachiyomi.extension.util.Missing").shouldBeNull()
    }

    @Test
    fun aClassLoaderThatCannotBeBuilt() {
        load(SOURCE_CLASS, sourceDir = null).shouldBeNull()
    }

    @Test
    fun metadataWithoutASourceClass() {
        shouldThrow<NullPointerException> { load(sourceClass = null) }
    }

    @Test
    fun theTestSourcesThemselves() {
        LoadedTestFactory().createSources().map { it.lang } shouldBe listOf("en", "fr")
        LoadedTestSource().name shouldBe "loaded"
        LoadedTestSource().id shouldBe 1L
        LoadedTestSource().supportsLatest shouldBe false
    }
}
