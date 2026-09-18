package exh.log

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class EHLogLevelInitTest {
    @After
    fun tearDown() {
        setCurrentLogLevel(null)
    }

    @Test
    fun initReadsTheDefaultPreferences() {
        val app = RuntimeEnvironment.getApplication()
        EHLogLevel.init(app)
        EHLogLevel.currentLogLevel shouldBe EHLogLevel.MINIMAL

        PreferenceManager.getDefaultSharedPreferences(app).edit { putInt("eh_log_level", EHLogLevel.EXTREME.ordinal) }
        EHLogLevel.init(app)
        EHLogLevel.currentLogLevel shouldBe EHLogLevel.EXTREME
    }
}
