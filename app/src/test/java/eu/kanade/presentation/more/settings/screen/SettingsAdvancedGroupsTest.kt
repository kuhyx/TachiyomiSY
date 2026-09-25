package eu.kanade.presentation.more.settings.screen

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.domain.base.BasePreferences.ExtensionInstaller
import eu.kanade.domain.source.service.SourcePreferences.DataSaver
import eu.kanade.presentation.more.settings.Preference.PreferenceItem.ListPreference
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SettingsAdvancedGroupsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val advanced = AdvancedScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        mockkStatic(
            "eu.kanade.tachiyomi.util.system.ContextExtensionsKt",
            "eu.kanade.tachiyomi.util.system.BuildConfigKt",
        )
        every { any<Context>().isShizukuInstalled } returns false
        koin.start(advanced.module())
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun installers(): Set<Any?> = (harness.item("Installer") as ListPreference<*>).entries.keys

    @Test
    fun privateInstallerOnDebugOnly() {
        harness.show(SettingsAdvancedScreen)
        installers() shouldContain ExtensionInstaller.PRIVATE
    }

    @Test
    fun privateInstallerOnPreview() {
        every { isDebugBuildType } returns false
        every { isPreviewBuildType } returns true
        harness.show(SettingsAdvancedScreen)
        installers() shouldContain ExtensionInstaller.PRIVATE
    }

    @Test
    fun privateInstallerHiddenOnStable() {
        every { isDebugBuildType } returns false
        every { isPreviewBuildType } returns false
        harness.show(SettingsAdvancedScreen)
        installers() shouldNotContain ExtensionInstaller.PRIVATE
    }

    @Test
    fun shizukuMissingShowsDialog() {
        harness.show(SettingsAdvancedScreen)
        harness.list("Installer", ExtensionInstaller.LEGACY) shouldBe true
        harness.list("Installer", ExtensionInstaller.SHIZUKU) shouldBe false
        harness.count("Shizuku") shouldBe 1
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        verify { harness.uriHandler.openUri("https://shizuku.rikka.app/download") }
        harness.list("Installer", ExtensionInstaller.SHIZUKU) shouldBe false
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        harness.count("Shizuku") shouldBe 0
    }

    @Test
    fun shizukuInstalledAccepted() {
        every { any<Context>().isShizukuInstalled } returns true
        harness.show(SettingsAdvancedScreen)
        harness.list("Installer", ExtensionInstaller.SHIZUKU) shouldBe true
        harness.click("Revoke trusted unknown extensions")
        verify { advanced.trust.revokeAll() }
    }

    @Test
    fun dataSaverFlags() {
        harness.show(SettingsAdvancedScreen)
        harness.item("Bandwidth Hero Proxy Server").enabled shouldBe false
        harness.item("Compress to Jpeg").enabled shouldBe false
        koin.source.dataSaver.set(DataSaver.BANDWIDTH_HERO)
        koin.source.dataSaverImageFormatJpeg.set(true)
        compose.waitForIdle()
        harness.item("Bandwidth Hero Proxy Server").enabled shouldBe true
        harness.item("Convert to Black And White").enabled shouldBe true
        harness.item("Compress to Jpeg").subtitle.toString().endsWith("Currently compresses to Jpeg") shouldBe true
        koin.source.dataSaver.set(DataSaver.WSRV_NL)
        compose.waitForIdle()
        harness.item("Compress to Jpeg").enabled shouldBe true
        harness.item("Convert to Black And White").enabled shouldBe false
    }
}
