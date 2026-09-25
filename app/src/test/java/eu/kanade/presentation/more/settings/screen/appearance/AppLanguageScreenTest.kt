package eu.kanade.presentation.more.settings.screen.appearance

import android.util.Xml
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.util.system.LocaleHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
internal class AppLanguageScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        unmockkAll()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    private fun show() {
        compose.setContent { MaterialTheme { Navigator(AppLanguageScreen()) } }
        compose.waitForIdle()
    }

    private fun locale(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    @Test
    fun pickLanguageThenDefault() {
        show()
        compose.onNodeWithText("App language").assertExists()
        compose.onAllNodesWithText("Default").onFirst().assertExists()
        compose.onAllNodesWithText("English").onFirst().performClick()
        compose.waitForIdle()
        locale() shouldBe "en"
        compose.onAllNodesWithText("Default").onFirst().performClick()
        compose.waitForIdle()
        locale() shouldBe ""
    }

    @Test
    fun startsFromChosenLocale() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        show()
        locale() shouldBe "en"
    }

    @Test
    fun blankNamesAreSkipped() {
        mockkObject(LocaleHelper)
        every { LocaleHelper.getLocalizedDisplayName(any()) } returns ""
        show()
        compose.onAllNodesWithText("English").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun localeTagsReadsNameOnly() {
        val parser = Xml.newPullParser()
        val xml = """<locale-config><locale other="x" name="de"/><item name="no"/><locale name="fr"/></locale-config>"""
        parser.setInput(StringReader(xml))
        parser.localeTags() shouldBe listOf("de", "fr")
    }
}
