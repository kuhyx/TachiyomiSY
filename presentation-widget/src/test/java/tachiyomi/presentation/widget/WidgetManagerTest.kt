package tachiyomi.presentation.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.LifecycleCoroutineScope
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.model.UpdatesWithRelations

private const val SETTLE_MS = 300L
private const val VERIFY_TIMEOUT_MS = 5_000L

/**
 * `WidgetManager.init` collects on `Dispatchers.Default`, so every expectation waits for the
 * redraw with a bounded `coVerify(timeout)`; a "nothing more happened" check settles first.
 */
@RunWith(RobolectricTestRunner::class)
internal class WidgetManagerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val getUpdates = mockk<GetUpdates>()
    private val updates = MutableSharedFlow<List<UpdatesWithRelations>>()
    private val scope = CoroutineScope(SupervisorJob())
    private val lifecycleScope = mockk<LifecycleCoroutineScope> {
        every { coroutineContext } returns scope.coroutineContext
    }

    @Before
    fun setUp() {
        WidgetInjekt.install(getUpdates)
        every { getUpdates.subscribe(read = false, after = any()) } returns updates
        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        coEvery { any<GlanceAppWidget>().updateAll(any()) } just Runs
    }

    @After
    fun tearDown() {
        scope.cancel()
        unmockkAll()
        WidgetInjekt.restore()
    }

    @Test
    fun redrawsBothWidgetsOnUpdates() {
        start()

        emit(listOf(updateOf(1)))

        awaitGridRedraws(1)
        awaitCoverScreenRedraws(1)
    }

    @Test
    fun ignoresSameChaptersReordered() {
        start()
        emit(listOf(updateOf(1, chapterId = 10), updateOf(2, chapterId = 20)))
        awaitGridRedraws(1)

        emit(listOf(updateOf(2, chapterId = 20), updateOf(1, chapterId = 10)))
        Thread.sleep(SETTLE_MS)

        coVerify(exactly = 1) { ofType<UpdatesGridGlanceWidget>().updateAll(context) }
    }

    @Test
    fun redrawsOnNewChapter() {
        start()
        emit(listOf(updateOf(1, chapterId = 10)))
        awaitGridRedraws(1)

        emit(listOf(updateOf(1, chapterId = 10), updateOf(1, chapterId = 11)))

        awaitGridRedraws(2)
        awaitCoverScreenRedraws(2)
    }

    @Test
    fun redrawsWhenTheLockFlips() {
        start()
        emit(listOf(updateOf(1)))
        awaitGridRedraws(1)

        WidgetInjekt.preferences.useAuthenticator.set(true)

        awaitGridRedraws(2)
    }

    @Test
    fun keepsRunningWhenARedrawFails() {
        coEvery { ofType<UpdatesGridGlanceWidget>().updateAll(any()) } throws IllegalStateException("no host")
        start()

        emit(listOf(updateOf(1)))
        awaitGridRedraws(1)
        emit(listOf(updateOf(2)))

        awaitGridRedraws(2)
        coVerify(exactly = 0) { ofType<UpdatesGridCoverScreenGlanceWidget>().updateAll(any()) }
    }

    private fun start() {
        with(WidgetManager(getUpdates, WidgetInjekt.preferences)) { context.init(lifecycleScope) }
        // The manager subscribes on a background dispatcher; a shared flow drops what nobody hears.
        runBlocking { updates.subscriptionCount.first { it > 0 } }
    }

    private fun emit(rows: List<UpdatesWithRelations>) {
        runBlocking { updates.emit(rows) }
    }

    private fun awaitGridRedraws(times: Int) {
        coVerify(timeout = VERIFY_TIMEOUT_MS, exactly = times) { ofType<UpdatesGridGlanceWidget>().updateAll(context) }
    }

    private fun awaitCoverScreenRedraws(times: Int) {
        coVerify(timeout = VERIFY_TIMEOUT_MS, exactly = times) {
            ofType<UpdatesGridCoverScreenGlanceWidget>().updateAll(context)
        }
    }
}
