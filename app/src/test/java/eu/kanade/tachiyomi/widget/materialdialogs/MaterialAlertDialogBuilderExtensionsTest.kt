package eu.kanade.tachiyomi.widget.materialdialogs

import android.content.Context
import android.content.ContextWrapper
import android.os.Looper
import android.view.ContextThemeWrapper
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** A themed context whose input method manager is missing, as on a device without one. */
private class NoImeContext(base: Context) : ContextWrapper(base) {
    override fun getSystemService(name: String): Any? =
        if (name == INPUT_METHOD_SERVICE) null else super.getSystemService(name)
}

@RunWith(RobolectricTestRunner::class)
internal class MaterialAlertDialogBuilderExtensionsTest {
    private val context: Context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_Tachiyomi)

    @Before
    fun setUp() {
        val preferences = BasePreferences(context, MapPreferenceStore())
        startKoin { modules(module { single { preferences } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun fieldOf(builder: MaterialAlertDialogBuilder): TextInputLayout {
        val dialog = builder.create()
        dialog.show()
        shadowOf(Looper.getMainLooper()).idle()
        return dialog.findViewById<TextInputLayout>(R.id.text_field).shouldNotBeNull()
    }

    @Test
    fun defaultsLeaveFieldEmpty() {
        val typed = mutableListOf<String>()
        val field = fieldOf(MaterialAlertDialogBuilder(context).setTextInput { typed += it })
        field.hint.shouldBeNull()
        val edit: EditText = field.editText.shouldNotBeNull()
        edit.text.toString() shouldBe ""
        edit.setText("abc")
        typed shouldBe listOf("abc")
    }

    @Test
    fun hintAndPrefillAreShown() {
        val field = fieldOf(
            MaterialAlertDialogBuilder(NoImeContext(context)).setTextInput(hint = "Tag", prefill = "a") { },
        )
        field.hint shouldBe "Tag"
        field.editText.shouldNotBeNull().text.toString() shouldBe "a"
    }
}
