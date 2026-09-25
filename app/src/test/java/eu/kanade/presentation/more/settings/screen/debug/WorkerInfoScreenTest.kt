package eu.kanade.presentation.more.settings.screen.debug

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
internal class WorkerInfoScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun work(workState: WorkInfo.State): WorkInfo = mockk {
        every { id } returns UUID(0, workState.ordinal.toLong())
        every { tags } returns setOf("tag-${workState.name}")
        every { state } returns workState
        every { nextScheduleTimeMillis } returns 0L
        every { runAttemptCount } returns 2
    }

    @Before
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single { UiPreferences(InMemoryPreferenceStore()) } }) }
        val manager = mockk<WorkManager>()
        // Queries come in the order the model declares them: finished, running, enqueued.
        every { manager.getWorkInfosFlow(any()) } returnsMany listOf(
            flowOf(listOf(work(WorkInfo.State.SUCCEEDED))),
            flowOf(emptyList()),
            flowOf(listOf(work(WorkInfo.State.ENQUEUED))),
        )
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        every { any<Context>().workManager } returns manager
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    @Test
    fun listsWorkAndCopies() {
        compose.setContent { MaterialTheme { Navigator(WorkerInfoScreen()) } }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("Attempt #3", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Worker info").assertExists()
        compose.onNodeWithContentDescription("Copy to clipboard").performClick()
        val clipboard = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copied = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        copied shouldContain "tag-SUCCEEDED"
        copied shouldContain "Next scheduled run"
        copied shouldContain "-\n"
    }
}
