package eu.kanade.presentation.browse.components

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.tachiyomi.extension.anAvailableExtension
import eu.kanade.tachiyomi.extension.anInstalledExtension
import eu.kanade.tachiyomi.extension.anUntrustedExtension
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
internal class ExtensionIconTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Written on the IO thread that loads the icon, read on the test thread.
    private val asked = AtomicInteger(0)

    @After
    fun tearDown() = unmockkAll()

    private fun packageWithIcon(): PackageInfo = PackageInfo().apply {
        packageName = context.packageName
        applicationInfo = ApplicationInfo(context.applicationInfo).apply { icon = android.R.drawable.btn_star }
    }

    @Test
    fun installedIconLoads() {
        mockkObject(ExtensionLoader)
        every { ExtensionLoader.getExtensionPackageInfo(any(), any()) } answers {
            asked.incrementAndGet()
            packageWithIcon().takeIf { secondArg<String>() == "pkg.good" }
        }
        compose.setContent {
            MaterialTheme {
                Column {
                    ExtensionIcon(anInstalledExtension(pkgName = "pkg.good"))
                    ExtensionIcon(anInstalledExtension(pkgName = "pkg.bad"), modifier = Modifier, density = 1)
                    ExtensionIcon(anAvailableExtension())
                    ExtensionIcon(anUntrustedExtension())
                }
            }
        }
        val info = packageWithIcon().applicationInfo!!
        context.packageManager.getResourcesForApplication(info)
            .getDrawableForDensity(info.icon, DisplayMetrics.DENSITY_DEFAULT, null)!!
            .toBitmap()
        compose.waitUntil(timeoutMillis = 20_000L) { asked.get() >= 2 }
        compose.waitForIdle()
        asked.get() shouldBe 2
    }
}
