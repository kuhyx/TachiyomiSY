package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
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

/** Opening a link, with and without forcing the system default browser. */
@RunWith(RobolectricTestRunner::class)
internal class ContextBrowserTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun opensAUrlInTheBrowser() {
        val started = slot<Intent>()
        val activityLike = spyk(context)
        every { activityLike.startActivity(capture(started)) } returns Unit
        activityLike.openInBrowser("https://example.test")
        activityLike.openInBrowser("https://example.test".toUri())
        started.captured.action shouldBe Intent.ACTION_VIEW
        started.captured.data.toString() shouldBe "https://example.test"
        started.captured.`package`.shouldBeNull()
    }

    @Test
    fun forcesTheDefaultBrowserWhen() {
        val started = slot<Intent>()
        val activityLike = spyk(context)
        every { activityLike.startActivity(capture(started)) } returns Unit
        stubBrowser("com.example.browser")
        activityLike.openInBrowser("https://example.test".toUri(), forceDefaultBrowser = true)
        started.captured.`package` shouldBe "com.example.browser"
    }

    @Test
    fun anUnresolvableBrowserIsIgnored() {
        val started = slot<Intent>()
        val activityLike = spyk(context)
        every { activityLike.startActivity(capture(started)) } returns Unit
        activityLike.openInBrowser("https://example.test".toUri(), forceDefaultBrowser = true)
        started.captured.`package`.shouldBeNull()
        val noActivity = ResolveInfo()
        val shadow = shadowOf(context.packageManager)
        shadow::class.java
            .getMethod("addResolveInfoForIntent", Intent::class.java, ResolveInfo::class.java)
            .invoke(shadow, Intent(Intent.ACTION_VIEW, "http://".toUri()), noActivity)
        activityLike.openInBrowser("https://example.test".toUri(), forceDefaultBrowser = true)
        started.captured.`package`.shouldBeNull()
    }

    @Test
    fun anInvalidDefaultBrowserIs() {
        val started = slot<Intent>()
        val activityLike = spyk(context)
        every { activityLike.startActivity(capture(started)) } returns Unit
        stubBrowser(DeviceUtil.invalidDefaultBrowsers.first())
        activityLike.openInBrowser("https://example.test".toUri(), forceDefaultBrowser = true)
        started.captured.`package`.shouldBeNull()
    }

    @Test
    fun browserFailuresAreToasted() {
        val broken = spyk(context)
        every { broken.startActivity(any()) } throws IllegalStateException("no browser")
        broken.openInBrowser("https://example.test")
        ShadowToast.getTextOfLatestToast() shouldBe "no browser"
    }

    // The deprecated shadow setter is reached dynamically so the build stays warning-free.
    private fun stubBrowser(name: String) {
        val info = ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = name } }
        val shadow = shadowOf(context.packageManager)
        shadow::class.java
            .getMethod("addResolveInfoForIntent", Intent::class.java, ResolveInfo::class.java)
            .invoke(shadow, Intent(Intent.ACTION_VIEW, "http://".toUri()), info)
    }
}
