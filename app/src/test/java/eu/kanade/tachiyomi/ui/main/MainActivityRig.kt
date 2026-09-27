package eu.kanade.tachiyomi.ui.main

import android.content.Intent
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.ui.base.disposeScreenModels
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.LibraryHarness
import exh.eh.EHentaiUpdateWorker
import exh.eh.scheduleBackground
import exh.log.EHLogLevel
import exh.source.BlacklistedSources
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowLooper
import tachiyomi.domain.storage.service.StoragePreferences
import java.util.concurrent.TimeUnit

/**
 * [MainActivity] launched for real over the library tab's Koin graph plus what the activity itself
 * injects; background work (E-Hentai updates, the chapter cache) is stubbed and recorded.
 */
internal class MainActivityRig {
    val harness: LibraryHarness = LibraryHarness()
    val indexing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val incognitoOf: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var incognitoAtStart: Boolean = false
    val chapterCache: ChapterCache = mockk(relaxed = true)
    val incognito: GetIncognitoState = mockk {
        every { await(any()) } answers { incognitoAtStart }
        every { subscribe(any()) } returns incognitoOf
    }
    private val hidden = BlacklistedSources.HIDDEN_SOURCES
    private val launched = mutableListOf<ActivityController<MainActivity>>()

    fun start() {
        // The library's spinner animates forever; an unpaused choreographer would feed it frames endlessly.
        ShadowChoreographer.setPaused(true)
        resetUiDispatcher()
        EHLogLevel.init(harness.app)
        every { harness.downloadCache.isInitializing } returns indexing
        // Pushed source and manga screens then wait on a loading screen instead of their own models.
        every { harness.sourceManager.isInitialized } returns MutableStateFlow(false)
        every { chapterCache.clear() } returns 0
        mockkStatic("exh.eh.EHentaiUpdateSchedulingKt")
        every { EHentaiUpdateWorker.scheduleBackground(any(), any(), any()) } just runs
        harness.basePreferences.shownOnboardingFlow.set(true)
        harness.start(
            module {
                single { SecurityPreferences(harness.store) }
                single { PrivacyPreferences(harness.store) }
                single { StoragePreferences(mockk(relaxed = true), harness.store) }
                single { incognito }
                single { chapterCache }
            },
        )
    }

    fun stop() {
        try {
            // A live activity keeps its home screen collecting the global tab channel into later classes.
            launched.forEach { it.pause().stop().destroy() }
            launched.clear()
            disposeScreenModels(HomeScreen, *HomeScreen.TABS.toTypedArray())
            harness.stop()
        } finally {
            unmockkAll()
            HomeScreen.showBottomNavEvent.tryReceive()
            BlacklistedSources.HIDDEN_SOURCES = hidden
            ShadowChoreographer.setPaused(false)
        }
    }

    /** Creates, starts and resumes the activity; [before] runs on it first, as the task's root. */
    fun launch(
        intent: Intent = Intent(harness.app, MainActivity::class.java),
        before: (MainActivity) -> Unit = {},
    ): ActivityController<MainActivity> {
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent)
        shadowOf(controller.get()).setIsTaskRoot(true)
        before(controller.get())
        launched += controller
        return controller.setup()
    }

    /**
     * Runs the main looper frame by frame until [condition] holds. The choreographer is paused, so only
     * advancing the clock hands Compose the frames its effects and recompositions wait for.
     */
    fun until(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + UNTIL_MS
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "condition not met in time" }
            ShadowLooper.idleMainLooper(FRAME_MS, TimeUnit.MILLISECONDS)
        }
    }
}

/** Runs [count] frames' worth of the main looper. */
internal fun MainActivityRig.frames(count: Int = 10) {
    repeat(count) { ShadowLooper.idleMainLooper(FRAME_MS, TimeUnit.MILLISECONDS) }
}

private const val UNTIL_MS = 10_000L
private const val FRAME_MS = 16L
