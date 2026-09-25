package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.view.View
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

/**
 * The pre-Android-9 side of the display helpers: on a plain JVM `Build.VERSION.SDK_INT` is 0, which is the
 * only way to reach the branches guarded by a minimum SDK.
 */
internal class DisplayLegacyExtensionsTest {

    @Test
    fun cutoutsAreUnknownBeforeAndroid() {
        val view = mockk<View>()
        every { view.rootWindowInsets } returns mockk(relaxed = true)
        view.hasDisplayCutout() shouldBe false
    }

    @Test
    fun theNavigationBarAlwaysNeedsA() {
        mockk<Context>().isNavigationBarNeedsScrim() shouldBe true
    }
}
