package androidx.preference.external

import android.content.Context
import androidx.preference.EditTextPreference
import androidx.preference.getOnBindEditTextListener
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Outside `androidx.preference` the package-private member is hidden, so this reaches the extension. */
@RunWith(RobolectricTestRunner::class)
internal class EditTextListenerExtensionTest {
    @Test
    fun theExtensionReadsTheListener() {
        val preference = EditTextPreference(ApplicationProvider.getApplicationContext<Context>())
        val listener = EditTextPreference.OnBindEditTextListener { }
        preference.setOnBindEditTextListener(listener)
        preference.getOnBindEditTextListener() shouldBe listener
    }
}
