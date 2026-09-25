package eu.kanade.tachiyomi.util.system

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/** Before Android 13 the app confirms a copy itself, and resolves the default browser the old way. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
internal class ContextClipboardLegacyTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun theDefaultBrowserIsResolvedThe() {
        val info = ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = "com.legacy.browser" } }
        val shadow = shadowOf(context.packageManager)
        shadow::class.java
            .getMethod("addResolveInfoForIntent", Intent::class.java, ResolveInfo::class.java)
            .invoke(shadow, Intent(Intent.ACTION_VIEW, "http://".toUri()), info)
        val started = slot<Intent>()
        val activityLike = spyk(context)
        every { activityLike.startActivity(capture(started)) } returns Unit
        activityLike.openInBrowser(url = "https://example.test", forceDefaultBrowser = true)
        started.captured.`package` shouldBe "com.legacy.browser"
    }

    @Test
    fun copyingToastsTheContent() {
        context.copyToClipboard("label", "a very long piece of content that the toast has to truncate in the middle")
        context.getSystemService(ClipboardManager::class.java).hasPrimaryClip() shouldBe true
        ShadowToast.getTextOfLatestToast() shouldContain "Copied"
    }
}
