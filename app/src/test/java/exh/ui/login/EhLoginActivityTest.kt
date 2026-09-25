package exh.ui.login

import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithText
import android.app.Activity
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import exh.source.ExhPreferences
import exh.ui.baseActivityModule
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.preference.InMemoryPreferenceStore

/** Koin and a WebView-capable package manager, up before the activity launches. */
internal class LoginActivityEnvironment : ExternalResource() {
    val preferences = ExhPreferences(InMemoryPreferenceStore())

    override fun before() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_WEBVIEW, true)
        stopKoin()
        startKoin { modules(baseActivityModule(context), module { single { preferences } }) }
    }

    override fun after() {
        stopKoin()
    }
}

@RunWith(RobolectricTestRunner::class)
internal class EhLoginActivityTest {
    private val environment = LoginActivityEnvironment()
    private val compose = createAndroidComposeRule<EhLoginActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(environment).around(compose)

    private fun webView(view: View = compose.activity.window.decorView): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { webView(view.getChildAt(it)) }
        else -> null
    }

    private fun advanced(option: String) {
        compose.onNodeWithText("Advanced").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(option).performClick()
        compose.waitForIdle()
    }

    private fun lastUrl(): String? = shadowOf(webView()).lastLoadedUrl

    @Test
    fun theLoginPageIsShown() {
        compose.waitForIdle()
        compose.onNodeWithText("ExHentai login").assertExists()
        lastUrl() shouldBe "https://forums.e-hentai.org/index.php?act=Login"
    }

    @Test
    fun recheckGoesToExhentai() {
        advanced("Recheck login status")
        lastUrl() shouldBe "https://exhentai.org/"
    }

    @Test
    fun theAlternativeLoginPage() {
        advanced("Alternative login page")
        lastUrl() shouldBe "https://e-hentai.org/bounce_login.php"
    }

    @Test
    fun skippingTheRestyle() {
        advanced("Skip page restyling")
        lastUrl() shouldBe "https://forums.e-hentai.org/index.php?act=Login&TEH_SKIP_INJECT=true"
    }

    @Test
    fun aCustomIgneousIsUsed() {
        advanced("Custom igneous cookie")
        compose.onNodeWithText(
            "Some users cannot access ExHentai the normal way, and have to pass in a specific igneous cookie " +
                "value, this option is for those users.",
        ).assertExists()
        compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput(" mine ")
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        val cookies = android.webkit.CookieManager.getInstance()
        cookies.setCookie("https://exhentai.org/", "ipb_member_id=1")
        cookies.setCookie("https://exhentai.org/", "ipb_pass_hash=2")
        val view = checkNotNull(webView())
        shadowOf(view).webViewClient.onPageFinished(view, "https://exhentai.org/")
        compose.waitForIdle()
        environment.preferences.igneousVal.get() shouldBe "mine"
        shadowOf(compose.activity).resultCode shouldBe Activity.RESULT_OK
        compose.activity.isFinishing shouldBe true
    }

    @Test
    fun theIgneousDialogCancels() {
        advanced("Custom igneous cookie")
        compose.onAllNodesWithText("Cancel").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Custom igneous cookie").assertDoesNotExist()
    }

    @Test
    fun closingFinishes() {
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
        compose.activity.isFinishing shouldBe true
    }
}
