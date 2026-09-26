package eu.kanade.presentation.more.settings.screen

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class SettingsDataScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val temp = TemporaryFolder()

    private val koin = SettingsKoin()
    private val data = DataScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        koin.start(data.module())
    }

    @After
    fun tearDown() {
        koin.stop()
    }

    private fun location(): String = harness.item("Storage location").subtitle.toString()

    @Test
    fun locationTextStates() {
        harness.show(SettingsDataScreen)
        location() shouldBe "No storage location set"
        koin.storage.baseStorageDirectory.set(Uri.fromFile(temp.root).toString())
        compose.waitForIdle()
        location() shouldEndWith temp.root.name
        koin.storage.baseStorageDirectory.set("zzz:nowhere")
        compose.waitForIdle()
        location() shouldBe "Invalid location: zzz:nowhere"
    }

    @Test
    fun pickerStoresChosenFolder() {
        val folder = Uri.fromFile(temp.root)
        harness.registry.answer = { folder }
        harness.show(SettingsDataScreen)
        harness.click("Storage location")
        koin.storage.baseStorageDirectory.get() shouldBe folder.toString()
    }

    @Test
    fun pickerIgnoresCancel() {
        harness.show(SettingsDataScreen)
        val before = koin.storage.baseStorageDirectory.get()
        harness.click("Storage location")
        koin.storage.baseStorageDirectory.get() shouldBe before
    }

    @Test
    fun pickerSurvivesDeniedGrant() {
        val resolver = mockk<ContentResolver> {
            every { takePersistableUriPermission(any(), any()) } throws SecurityException("no grant")
        }
        val app = ApplicationProvider.getApplicationContext<Context>()
        val context = object : ContextWrapper(app) {
            override fun getContentResolver(): ContentResolver = resolver
        }
        val folder = Uri.fromFile(temp.root)
        harness.registry.answer = { folder }
        harness.show(SettingsDataScreen, context = context)
        harness.click("Storage location")
        ShadowToast.getTextOfLatestToast().toString() shouldStartWith "Failed to acquire"
        koin.storage.baseStorageDirectory.get() shouldBe folder.toString()
    }

    @Test
    fun pickerMissingShowsToast() {
        harness.registry.failure = ActivityNotFoundException()
        harness.show(SettingsDataScreen)
        harness.click("Storage location")
        ShadowToast.getTextOfLatestToast().toString() shouldBe "No file picker app found"
    }

    @Test
    fun pickerSkipsUnopenableUri() {
        val resolver = mockk<ContentResolver>(relaxed = true)
        val app = ApplicationProvider.getApplicationContext<Context>()
        val context = object : ContextWrapper(app) {
            override fun getContentResolver(): ContentResolver = resolver
        }
        harness.registry.answer = { Uri.parse("zzz:nowhere") }
        harness.show(SettingsDataScreen, context = context)
        val before = koin.storage.baseStorageDirectory.get()
        harness.click("Storage location")
        koin.storage.baseStorageDirectory.get() shouldBe before
    }

    @Test
    fun helpActionOpensGuide() {
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides harness.uriHandler) {
                MaterialTheme { Row { with(SettingsDataScreen) { AppBarAction() } } }
            }
        }
        compose.onNodeWithContentDescription("Tracking guide").performClick()
        verify { harness.uriHandler.openUri(SettingsDataScreen.HELP_URL) }
    }
}
