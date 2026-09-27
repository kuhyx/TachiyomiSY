package eu.kanade.tachiyomi.util

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.util.storage.getUriCompat
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.io.File

/** A successful dump: the logcat command runs and the log file is offered through the share sheet. */
@RunWith(RobolectricTestRunner::class)
internal class CrashLogUtilShareTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val extensionManager = mockk<ExtensionManager>()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { extensionManager.installedExtensionsFlow } returns MutableStateFlow(emptyList())
        every { extensionManager.availableExtensionsFlow } returns MutableStateFlow(emptyList())
        mockkStatic("eu.kanade.tachiyomi.util.storage.FileExtensionsKt")
        every { any<File>().getUriCompat(any()) } returns Uri.parse("content://logs/crash.txt")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun logsAreShared() = runTest(dispatcher) {
        val util = CrashLogUtil(
            context = context,
            extensionManager = extensionManager,
            preferences = BasePreferences(context, InMemoryPreferenceStore()),
            logcatCommand = { arrayOf("true") },
        )
        util.dumpLogs()
        val shared = shadowOf(context).nextStartedActivity
        shared.action shouldBe Intent.ACTION_CHOOSER
    }
}
