package eu.kanade.tachiyomi.util.system

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.WindowInsets
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TabletUiMode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class DisplayExtensionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uiPreferences = UiPreferences(InMemoryPreferenceStore())

    @Before
    fun setUp() {
        startKoin { modules(module { single { uiPreferences } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun tabletUiNeedsASevenTwentyDp() {
        Configuration().apply { smallestScreenWidthDp = 719 }.isTabletUi() shouldBe false
        Configuration().apply { smallestScreenWidthDp = 720 }.isTabletUi() shouldBe true
    }

    @Test
    fun automaticModeUsesThePer() {
        uiPreferences.tabletUiMode.set(TabletUiMode.AUTOMATIC)
        phoneContext()
            .prepareTabletUiContext()
            .resources
            .configuration
            .isTabletUi() shouldBe false
        wideContext(700, Configuration.ORIENTATION_PORTRAIT)
            .prepareTabletUiContext()
            .resources
            .configuration
            .isTabletUi() shouldBe true
        wideContext(600, Configuration.ORIENTATION_LANDSCAPE)
            .prepareTabletUiContext()
            .resources
            .configuration
            .isTabletUi() shouldBe true
        wideContext(599, Configuration.ORIENTATION_LANDSCAPE)
            .prepareTabletUiContext()
            .resources
            .configuration
            .isTabletUi() shouldBe false
    }

    @Test
    fun theOtherModesForceTheAnswer() {
        uiPreferences.tabletUiMode.set(TabletUiMode.ALWAYS)
        phoneContext()
            .prepareTabletUiContext()
            .resources
            .configuration
            .isTabletUi() shouldBe true
        uiPreferences.tabletUiMode.set(TabletUiMode.NEVER)
        wideContext(900, Configuration.ORIENTATION_PORTRAIT)
            .prepareTabletUiContext()
            .resources
            .configuration
            .isTabletUi() shouldBe false
        uiPreferences.tabletUiMode.set(TabletUiMode.LANDSCAPE)
        phoneContext(Configuration.ORIENTATION_LANDSCAPE)
            .prepareTabletUiContext()
            .resources
            .configuration
            .isTabletUi() shouldBe true
        phoneContext(Configuration.ORIENTATION_PORTRAIT)
            .prepareTabletUiContext()
            .resources
            .configuration
            .isTabletUi() shouldBe false
    }

    @Test
    fun anUnchangedContextIsReturnedAs() {
        uiPreferences.tabletUiMode.set(TabletUiMode.NEVER)
        val phone = phoneContext()
        phone.prepareTabletUiContext() shouldBeSameInstanceAs phone
    }

    @Test
    fun nightModeFollowsThe() {
        context.isNightMode() shouldBe false
        val night = Configuration(context.resources.configuration).apply {
            uiMode = Configuration.UI_MODE_NIGHT_YES
        }
        context.createConfigurationContext(night).isNightMode() shouldBe true
    }

    @Test
    fun displayCutoutsComeFromThe() {
        val view = mockk<View>()
        every { view.rootWindowInsets } returns null
        view.hasDisplayCutout() shouldBe false
        val insets = mockk<WindowInsets> { every { displayCutout } returns mockk() }
        every { view.rootWindowInsets } returns insets
        view.hasDisplayCutout() shouldBe true
        val activity = mockk<Activity>()
        every { activity.window } returns mockk { every { decorView } returns view }
        activity.hasDisplayCutout() shouldBe true
    }

    @Test
    fun theNavigationBarScrimFollows() {
        mockkObject(InternalResourceHelper)
        every { InternalResourceHelper.getBoolean(context, "config_navBarNeedsScrim", true) } returns true
        context.isNavigationBarNeedsScrim() shouldBe true
        every { InternalResourceHelper.getBoolean(context, "config_navBarNeedsScrim", true) } returns false
        context.isNavigationBarNeedsScrim() shouldBe false
        unmockkAll()
    }

    private fun phoneContext(orientation: Int = Configuration.ORIENTATION_PORTRAIT): Context =
        wideContext(400, orientation)

    private fun wideContext(widthDp: Int, orientation: Int): Context {
        val configuration = Configuration(context.resources.configuration).apply {
            smallestScreenWidthDp = widthDp
            this.orientation = orientation
        }
        return context.createConfigurationContext(configuration)
    }
}
