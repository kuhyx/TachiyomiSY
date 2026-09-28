package mihon.app.shizuku

import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.ParcelFileDescriptor
import eu.kanade.tachiyomi.BuildConfig
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemProperties
import org.robolectric.util.ReflectionHelpers
import java.io.File

private const val ID = BuildConfig.APPLICATION_ID

@RunWith(RobolectricTestRunner::class)
internal class ShellInterfaceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val sdk = Build.VERSION.SDK_INT
    private lateinit var target: File
    private lateinit var session: FakeSession
    private lateinit var installer: FakeInstaller

    @Before
    fun setUp() {
        target = File(folder.root, "written.apk")
        session = FakeSession(target)
        installer = FakeInstaller(session)
        installShellFakes(installer)
        // Robolectric's `Os.write` never completes a FileBridge write, so the plain fd path is the default.
        ShadowSystemProperties.override("fw.revocable_fd", "true")
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        ShadowSystemProperties.override("fw.revocable_fd", "false")
        unmockkAll()
    }

    private fun apk(): AssetFileDescriptor {
        val file = folder.newFile("source.apk")
        file.writeText("apk bytes")
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(fd, 0L, file.length())
    }

    private fun installOn(level: Int) {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", level)
        ShellInterface().install(apk())
    }

    @Test
    fun installWritesAndCommits() {
        installOn(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        target.readText() shouldBe "apk bytes"
        installer.created shouldContainExactly listOf("4:$ID:$ID:0")
        session.commits.first() shouldBe "write:extension:0:9"
        session.commits.last().endsWith(":false") shouldBe true
        val flags = checkNotNull(installer.params).let { it::class.java.getField("installFlags").getInt(it) }
        (flags and 2) shouldBe 2
    }

    @Test
    fun plainFdUsesAFileBridge() {
        ShadowSystemProperties.override("fw.revocable_fd", "false")
        stubIoBridge()
        installOn(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        session.commits.last().endsWith(":false") shouldBe true
    }

    @Test
    fun tiramisuSetsPackageSource() {
        installOn(Build.VERSION_CODES.TIRAMISU)
        installer.created.single().startsWith("4:") shouldBe true
    }

    @Test
    fun androidElevenUsesOldSession() {
        installOn(Build.VERSION_CODES.R)
        installer.created shouldContainExactly listOf("3:$ID:0")
    }

    @Test
    fun oreoCommitsWithoutTransfer() {
        installOn(Build.VERSION_CODES.O)
        session.commits.last().endsWith(":false") shouldBe false
    }

    @Test
    fun destroyExitsTheProcess() {
        val exits = mutableListOf<Int>()
        ShellInterface(exit = { exits += it }).destroy()
        exits shouldContainExactly listOf(0)
    }
}
