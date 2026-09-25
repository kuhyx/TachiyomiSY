package eu.kanade.tachiyomi

import androidx.preference.PreferenceManager
import com.elvishew.xlog.XLog
import com.hippo.unifile.UniFile
import exh.log.EHLogLevel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.storage.service.StorageManager
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class AppLoggingTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val app = attachedApp()
    private val storage = mockk<StorageManager>()

    @Before
    fun setUp() {
        startKoin { modules(module { single { storage } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun logAt(level: EHLogLevel) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putInt("eh_log_level", level.ordinal).commit()
    }

    @Test
    fun extremeWritesTheLogFile() {
        logAt(EHLogLevel.EXTREME)
        every { storage.getLogsDirectory() } returns UniFile.fromFile(folder.root)
        app.setupExhLogging()
        XLog.d("probe")
        val file = writtenLog()
        file.name shouldEndWith "-${BuildConfig.BUILD_TYPE}.txt"
        file.readText() shouldContain "D/"
    }

    // EnhancedFilePrinter writes on its own worker thread.
    private fun writtenLog(): File {
        repeat(times = 200) {
            val file = folder.root.listFiles().orEmpty().singleOrNull()
            if (file != null && file.readText().contains("probe")) return file
            Thread.sleep(25)
        }
        error("no log file with the probe in ${folder.root}")
    }

    @Test
    fun extraLogsDebugWithoutFolder() {
        logAt(EHLogLevel.EXTRA)
        every { storage.getLogsDirectory() } returns null
        app.setupExhLogging(debug = false)
        EHLogLevel.currentLogLevel shouldBe EHLogLevel.EXTRA
    }

    @Test
    fun minimalReleaseLogsWarnings() {
        logAt(EHLogLevel.MINIMAL)
        every { storage.getLogsDirectory() } returns null
        app.setupExhLogging(debug = false)
        EHLogLevel.currentLogLevel shouldBe EHLogLevel.MINIMAL
    }

    @Test
    fun minimalDebugLogsDebug() {
        logAt(EHLogLevel.MINIMAL)
        every { storage.getLogsDirectory() } returns null
        app.setupExhLogging(debug = true)
        EHLogLevel.currentLogLevel shouldBe EHLogLevel.MINIMAL
    }
}
