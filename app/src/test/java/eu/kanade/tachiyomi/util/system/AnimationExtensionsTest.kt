package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AnimationExtensionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun readsTheAnimatorScaleSetting() {
        context.animatorDurationScale shouldBe 1f
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0.5f)
        context.animatorDurationScale shouldBe 0.5f
    }
}
