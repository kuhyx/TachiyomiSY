package eu.kanade.tachiyomi.ui.main

import android.app.assist.AssistContent
import android.content.Intent
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.OnboardingScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import exh.eh.EHentaiUpdateWorker
import exh.eh.scheduleBackground
import exh.source.BlacklistedSources
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import tachiyomi.core.common.Constants

/** The activity launched for real: its first launch, incoming intents, and the task-root check. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MainActivityTest {
    private val rig = MainActivityRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun launchShowsHome() {
        rig.harness.libraryPreferences.autoClearChapterCache.set(true)
        rig.harness.basePreferences.incognitoMode.set(true)
        rig.harness.exhPreferences.isHentaiEnabled.set(false)
        val activity = rig.launch().get()
        rig.until { activity.navigator != null }
        activity.navigator?.lastItem.shouldBeInstanceOf<HomeScreen>()
        rig.until { !rig.harness.basePreferences.incognitoMode.get() }
        verify(timeout = 5_000) { rig.chapterCache.clear() }
        BlacklistedSources.HIDDEN_SOURCES.contains(EH_SOURCE_ID) shouldBe true
    }

    @Test
    fun shortcutIntentIsHandled() {
        val intent = Intent(rig.harness.app, MainActivity::class.java).setAction(Constants.SHORTCUT_LIBRARY)
        val activity = rig.launch(intent).get()
        rig.until { activity.ready }
    }

    @Test
    fun newIntentsAreRouted() {
        val controller = rig.launch()
        rig.until { controller.get().navigator != null }
        controller.newIntent(Intent(Intent.ACTION_APPLICATION_PREFERENCES))
        rig.until { controller.get().navigator?.lastItem is SettingsScreen }
    }

    @Test
    fun onboardingIsShownOnce() {
        rig.harness.basePreferences.shownOnboardingFlow.set(false)
        val activity = rig.launch().get()
        rig.until { activity.navigator?.lastItem is OnboardingScreen }
    }

    @Test
    fun assistUrlFollowsTopScreen() {
        val activity = rig.launch().get()
        AssistContent().also(activity::onProvideAssistContent).webUri.shouldBeNull()
        rig.until { activity.navigator != null }
        AssistContent().also(activity::onProvideAssistContent).webUri.shouldBeNull()
        activity.navigator!!.push(AssistingScreen(null))
        AssistContent().also(activity::onProvideAssistContent).webUri.shouldBeNull()
        activity.navigator!!.push(AssistingScreen("https://example.org/a"))
        AssistContent().also(activity::onProvideAssistContent).webUri.toString() shouldBe "https://example.org/a"
    }

    @Test
    fun incognitoOffPopsSourceScreens() {
        val activity = rig.launch().get()
        rig.until { activity.navigator != null }
        val navigator = activity.navigator!!
        fun toggle() {
            rig.harness.basePreferences.incognitoMode.set(true)
            rig.frames()
            rig.harness.basePreferences.incognitoMode.set(false)
        }
        navigator.push(MangaScreen(1L, fromSource = false))
        toggle()
        rig.frames()
        navigator.lastItem.shouldBeInstanceOf<MangaScreen>()
        navigator.push(MangaScreen(1L, fromSource = true))
        toggle()
        rig.until { navigator.lastItem is HomeScreen }
        navigator.push(BrowseSourceScreen(1L, ""))
        rig.until { rig.incognitoOf.subscriptionCount.value > 0 }
        toggle()
        rig.until { navigator.lastItem is HomeScreen }
    }

    @Test
    fun bannersFollowState() {
        rig.incognitoAtStart = true
        val activity = rig.launch().get()
        rig.until { activity.navigator != null }
        rig.harness.basePreferences.downloadedOnly.set(true)
        rig.frames()
        rig.indexing.value = true
        rig.frames()
        rig.indexing.value = false
        rig.harness.basePreferences.downloadedOnly.set(false)
        rig.incognitoOf.value = false
        rig.frames()
        activity.isFinishing shouldBe false
    }

    @Test
    fun idleTasksRunAfterFirstPaint() {
        rig.harness.exhPreferences.enableExhentai.set(true)
        val controller = rig.launch { activity ->
            MainActivity::class.java.getDeclaredField("firstPaint").also { it.isAccessible = true }.set(activity, true)
        }
        rig.until { controller.get().runExhConfigureDialog || controller.get().navigator != null }
        verify(timeout = 5_000) { EHentaiUpdateWorker.scheduleBackground(controller.get(), any(), any()) }
        var ran = false
        controller.get().initWhenIdle { ran = true }
        ran shouldBe true
    }

    @Test
    fun onlyTheTaskRootStays() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        shadowOf(controller.get()).setIsTaskRoot(false)
        controller.create()
        controller.get().isFinishing shouldBe true
    }
}
