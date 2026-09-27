package eu.kanade.tachiyomi.extension.util

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.spyk
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A source class that loads in the sandbox, so the loader's "is a Source" arms run. */
internal open class SandboxSource : EmptyTestSource() {
    override val lang: String = "de"
}

/** A factory that loads in the sandbox. */
internal open class SandboxFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(SandboxSource())
}

/**
 * The extension class loader asks the JVM's system loader first, which knows every class on the test
 * classpath (as uninstrumented copies of other types). Classes generated at runtime under new names
 * are unknown to it, so the lookup falls through to the context's loader, which is the sandbox's.
 */
@RunWith(RobolectricTestRunner::class)
internal class ExtensionLoaderSourcesLoadedTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun generatedLoader(): ClassLoader {
        val generated = ByteBuddy()
            .subclass(SandboxSource::class.java)
            .name(GENERATED_SOURCE)
            .make()
            .include(ByteBuddy().subclass(SandboxFactory::class.java).name(GENERATED_FACTORY).make())
            .load(requireNotNull(SandboxSource::class.java.classLoader), ClassLoadingStrategy.Default.WRAPPER)
            .loaded
        return requireNotNull(generated.classLoader)
    }

    private fun load(sourceClass: String): List<String>? {
        val context = spyk<Context>(app)
        val loader = generatedLoader()
        every { context.classLoader } returns loader
        val pkgInfo = extensionPackage(metaData = extensionMetaData(sourceClass = sourceClass))
        return ExtensionLoader.loadSources(
            context = context,
            pkgInfo = pkgInfo,
            appInfo = requireNotNull(pkgInfo.applicationInfo),
            extName = "Ext Name",
        )?.map { it.lang }
    }

    @Test
    fun sandboxSourcesLoad() {
        load(GENERATED_SOURCE) shouldBe listOf("de")
        load("$GENERATED_SOURCE;$GENERATED_FACTORY") shouldBe listOf("de", "de")
    }

    private companion object {
        const val GENERATED_SOURCE = "eu.kanade.tachiyomi.extension.generated.GeneratedSource"
        const val GENERATED_FACTORY = "eu.kanade.tachiyomi.extension.generated.GeneratedFactory"
    }
}
