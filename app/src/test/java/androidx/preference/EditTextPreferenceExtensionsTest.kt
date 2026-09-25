package androidx.preference

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EditTextPreferenceExtensionsTest {

    @Test
    fun readsThePackagePrivateListener() {
        val preference = EditTextPreference(ApplicationProvider.getApplicationContext<Context>())
        preference.getOnBindEditTextListener().shouldBeNull()
        val listener = EditTextPreference.OnBindEditTextListener { }
        preference.setOnBindEditTextListener(listener)
        preference.getOnBindEditTextListener() shouldBe listener
    }
}
