package exh.util

import android.app.Application
import android.app.job.JobScheduler
import android.content.ClipboardManager
import android.content.res.Configuration
import android.net.wifi.WifiManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
internal class ContextExtensionsTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val sdk = Build.VERSION.SDK_INT

    @After
    fun restoreSdk() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
    }

    @Test
    fun systemServicesResolve() {
        context.wifiManager.shouldBeInstanceOf<WifiManager>()
        context.clipboardManager.shouldBeInstanceOf<ClipboardManager>()
        context.jobScheduler.shouldBeInstanceOf<JobScheduler>()
    }

    @Test
    fun nightModeFollowsConfiguration() {
        val config = context.resources.configuration
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        context.isInNightMode.shouldBeTrue()
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        context.isInNightMode.shouldBeFalse()
    }

    @Test
    fun wakeLockIsCreated() {
        context.createPartialWakeLock("test:wake").shouldNotBeNull()
    }

    @Test
    fun wifiLockPerSdk() {
        context.createWifiLock("test:wifi").shouldNotBeNull()
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.P)
        context.createWifiLock("test:wifi-legacy").shouldNotBeNull()
    }
}
