package exh.log

import android.app.Application
import android.view.Choreographer
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.source.service.SourcePreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class EHDebugModeOverlayTest {
    @get:Rule
    val compose = createComposeRule()

    private val preferences = SourcePreferences(InMemoryPreferenceStore())

    private val postedFpsCallbacks = mutableListOf<Choreographer.FrameCallback>()
    private lateinit var choreographer: Choreographer

    @Before
    fun setUp() {
        // The fps counter reposts a frame callback from every frame, so the looper would never be
        // idle again; hold its callbacks here instead and run them by hand.
        choreographer = spyk(Choreographer.getInstance())
        every { choreographer.postFrameCallback(any()) } answers {
            val callback = firstArg<Choreographer.FrameCallback>()
            if (callback::class.java.name.contains("FpsState")) postedFpsCallbacks += callback else callOriginal()
        }
        mockkStatic(Choreographer::class)
        every { Choreographer.getInstance() } returns choreographer
        EHLogLevel.init(ApplicationProvider.getApplicationContext<Application>())
        stopKoin()
        startKoin { modules(module { single { preferences } }) }
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    @Test
    fun overlayListsBuildInfo() {
        preferences.enableSourceBlacklist.set(false)
        compose.setContent { DebugModeOverlay() }
        compose.onNodeWithText("Build type: debug", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Debug mode: enabled", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Source blacklist: disabled", substring = true).assertIsDisplayed()
        compose.onNodeWithText("fps: 0.0").assertIsDisplayed()
    }

    /** Frames spanning more than the counter's interval replace its value. */
    @Test
    fun framesUpdateTheFpsCounter() {
        preferences.enableSourceBlacklist.set(true)
        compose.setContent { DebugModeOverlay() }
        var frameTimeNanos = 0L
        repeat(FRAMES) {
            // removeAt, not removeLast(): Kotlin binds the latter to SequencedCollection on a newer
            // JDK than the one the tests run on (NoSuchMethodError under Java 17).
            postedFpsCallbacks.removeAt(postedFpsCallbacks.lastIndex).doFrame(frameTimeNanos)
            frameTimeNanos += FRAME_MILLIS * NANOS_PER_MILLI
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("fps: 0.0").assertCountEquals(0)
        compose.onNodeWithText("Source blacklist: enabled", substring = true).assertIsDisplayed()
    }

    /** A composition that fails after the counter was remembered abandons it instead of forgetting it. */
    @Test
    fun abandonedCounterIsUnregistered() {
        shouldThrow<IllegalStateException> {
            compose.setContent {
                DebugModeOverlay()
                error("abandon")
            }
        }
        postedFpsCallbacks.size shouldBe 0
        verify(atLeast = 1) { choreographer.removeFrameCallback(any()) }
    }

    @Test
    fun appendItemBreaksLineByDefault() {
        val text = buildAnnotatedString {
            appendItem("a:", "b")
            appendItem("c:", "d", newLine = false)
        }
        text.text shouldBe "a: b\nc: d"
        text.spanStyles.size shouldBe 2
    }

    private companion object {
        const val FRAMES = 80
        const val FRAME_MILLIS = 20L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
