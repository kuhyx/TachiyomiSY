package tachiyomi.core.common.storage

import android.content.Context
import androidx.core.net.toUri
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowEnvironment
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.nio.file.Files
import java.nio.file.Path

@RunWith(RobolectricTestRunner::class)
internal class AndroidStorageFolderProviderTest {
    private lateinit var context: Context
    private lateinit var external: Path
    private lateinit var provider: FolderProvider

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        external = Files.createTempDirectory("external")
        ShadowEnvironment.setExternalStorageDirectory(external)
        provider = AndroidStorageFolderProvider(context)
    }

    @Test
    fun directoryIsUnderExternalRoot() {
        val directory = provider.directory()
        directory.parentFile?.toPath() shouldBe external.toAbsolutePath()
        directory.name shouldBe context.stringResource(MR.strings.app_name)
    }

    @Test
    fun pathIsTheDirectoryUri() {
        provider.path() shouldBe provider.directory().toUri().toString()
        provider.path() shouldBe "file://${provider.directory().absolutePath}"
    }
}
