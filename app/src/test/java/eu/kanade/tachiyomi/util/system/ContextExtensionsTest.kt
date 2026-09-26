package eu.kanade.tachiyomi.util.system

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.PowerManager
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class ContextExtensionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun clipboardSkipsBlankContentAnd() {
        context.copyToClipboard("label", " ")
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.hasPrimaryClip() shouldBe false
        context.copyToClipboard("label", "content")
        clipboard.primaryClip!!.getItemAt(0).text shouldBe "content"
        ShadowToast.shownToastCount() shouldBe 0
    }

    @Test
    fun clipboardFailuresAreToasted() {
        val broken = spyk(context)
        every { broken.getSystemService(ClipboardManager::class.java) } returns null
        broken.copyToClipboard("label", "content")
        ShadowToast.getTextOfLatestToast() shouldContain "clipboard"
    }

    @Test
    fun powerManagerComesFromThe() {
        context.powerManager shouldBe context.getSystemService(PowerManager::class.java)
    }

    @Test
    fun createsAndReplacesCacheFiles() {
        val first = context.createFileInCacheDir("logs.txt")
        first.exists() shouldBe true
        first.writeText("old")
        val second = context.createFileInCacheDir("logs.txt")
        second.readText() shouldBe ""
        second shouldBe File(context.externalCacheDir, "logs.txt")
    }

    @Test
    fun readsTheSizeOfAUri() {
        val file = context.createFileInCacheDir("size.txt").apply { writeText("12345") }
        context.getUriSize(file.toUri()) shouldBe 5L
        context.getUriSize("content://missing/1".toUri()).shouldBeNull()
        val empty = context.createFileInCacheDir("empty.txt")
        context.getUriSize(empty.toUri()) shouldBe 0L
        // A scheme UniFile cannot open at all.
        context.getUriSize("https://example.test/a".toUri()).shouldBeNull()
    }

    @Test
    fun packageChecksAndTheInstall() {
        context.isPackageInstalled("com.example.missing") shouldBe false
        context.isShizukuInstalled shouldBe false
        shadowOf(context.packageManager).installPackage(packageInfo("com.miui.packageinstaller"))
        context.hasMiuiPackageInstaller shouldBe true
        context.isShizukuInstalled shouldBe false
        shadowOf(context.packageManager).installPackage(packageInfo("moe.shizuku.privileged.api"))
        context.isShizukuInstalled shouldBe true
        val started = slot<Intent>()
        val activityLike = spyk(context)
        every { activityLike.startActivity(capture(started)) } returns Unit
        activityLike.launchInstallPermissionRequest()
        started.captured.data.toString() shouldContain context.packageName
    }

    private fun packageInfo(name: String) = PackageInfo().apply {
        packageName = name
        applicationInfo = ApplicationInfo().apply { packageName = name }
    }
}
