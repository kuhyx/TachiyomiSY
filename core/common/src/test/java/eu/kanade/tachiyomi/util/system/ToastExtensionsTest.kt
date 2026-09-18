package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.widget.Toast
import dev.icerock.moko.resources.StringResource
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class ToastExtensionsTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun textToastIsShortByDefault() {
        val toast = context.toast("hello")
        toast.duration shouldBe Toast.LENGTH_SHORT
        ShadowToast.getTextOfLatestToast() shouldBe "hello"
        ShadowToast.getLatestToast() shouldBe toast
    }

    @Test
    fun nullTextBecomesEmpty() {
        context.toast(null)
        ShadowToast.getTextOfLatestToast() shouldBe ""
    }

    @Test
    fun durationAndBlockAreApplied() {
        var customised: Toast? = null
        val toast = context.toast("long", Toast.LENGTH_LONG) { customised = it }
        toast.duration shouldBe Toast.LENGTH_LONG
        customised shouldBe toast
        ShadowToast.shownToastCount() shouldBe 1
    }

    @Test
    fun resourceToastResolvesTheString() {
        val toast = context.toast(StringResource(android.R.string.ok))
        ShadowToast.getTextOfLatestToast() shouldBe context.getString(android.R.string.ok)
        toast.duration shouldBe Toast.LENGTH_SHORT
    }

    @Test
    fun resourceToastPassesOptions() {
        var seen = 0
        val toast = context.toast(StringResource(android.R.string.cancel), Toast.LENGTH_LONG) { seen++ }
        toast.duration shouldBe Toast.LENGTH_LONG
        seen shouldBe 1
    }
}
