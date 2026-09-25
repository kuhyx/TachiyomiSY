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
        val files = folder.root.listFiles().orEmpty()
        files.size shouldBe 1
        files[0].name shouldEndWith "-${BuildConfig.BUILD_TYPE}.txt"
        files[0].readText() shouldContain "D/"
        files[0].readText() shouldContain "probe"
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
