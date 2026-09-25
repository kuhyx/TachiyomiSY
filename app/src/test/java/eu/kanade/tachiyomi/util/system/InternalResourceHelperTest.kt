package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InternalResourceHelperTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun knownFlagsComeFromTheSystem() {
        val system = Resources.getSystem()
        val id = system.getIdentifier("config_navBarNeedsScrim", "bool", "android")
        InternalResourceHelper.getBoolean(context, "config_navBarNeedsScrim", true) shouldBe system.getBoolean(id)
        InternalResourceHelper.getBoolean(context, "config_does_not_exist_at_all", true) shouldBe true
        InternalResourceHelper.getBoolean(context, "config_does_not_exist_at_all", false) shouldBe false
    }
}
