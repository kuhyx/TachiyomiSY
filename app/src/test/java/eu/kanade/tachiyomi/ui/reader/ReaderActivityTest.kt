package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ReaderActivityTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val harness = ReaderVmHarness(app)

    @Before
    fun setUp() {
        harness.start(module { single { SecurityPreferences(harness.store) } })
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun missingExtrasFinish() {
        val controller = Robolectric.buildActivity(ReaderActivity::class.java, Intent(app, ReaderActivity::class.java))
        val activity = controller.setup().get()
        activity.isFinishing shouldBe true
    }
}
