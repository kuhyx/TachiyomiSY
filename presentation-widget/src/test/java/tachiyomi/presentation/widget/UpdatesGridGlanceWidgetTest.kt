package tachiyomi.presentation.widget

import android.content.Context
import android.util.SizeF
import androidx.compose.ui.unit.dp
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.appwidget.SizeMode
import androidx.glance.testing.unit.hasText
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.presentation.widget.util.dayNightColorResource
import java.time.ZonedDateTime

private const val FAILING_MANGA_ID = 2L

@RunWith(RobolectricTestRunner::class)
internal class UpdatesGridGlanceWidgetTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val getUpdates = mockk<GetUpdates>()
    private val requestedMangaIds = mutableListOf<Long>()

    @Before
    fun setUp() {
        WidgetInjekt.install(getUpdates)
        installImageLoader { request ->
            val mangaId = (request.data as MangaCover).mangaId
            requestedMangaIds += mangaId
            if (mangaId == FAILING_MANGA_ID) errorFor(request) else successFor(request, coverBitmap())
        }
    }

    @After
    fun tearDown() {
        resetImageLoader()
        WidgetInjekt.restore()
    }

    @Test
    fun usesThemedColoursNoPadding() {
        val widget = UpdatesGridGlanceWidget()

        widget.sizeMode shouldBe SizeMode.Exact
        widget.foreground shouldBe context.dayNightColorResource(R.color.appwidget_on_secondary_container)
        widget.background.shouldBeInstanceOf<AndroidResourceImageProvider>().resId shouldBe
            R.drawable.appwidget_background
        widget.topPadding shouldBe 0.dp
        widget.bottomPadding shouldBe 0.dp
    }

    @Test
    fun receiverBuildsTheWidget() {
        UpdatesGridGlanceReceiver().glanceAppWidget.shouldBeInstanceOf<UpdatesGridGlanceWidget>()
    }

    @Test
    fun dateLimitIsThreeMonthsAgo() {
        val expected = ZonedDateTime.now().minusMonths(3).toInstant().toEpochMilli()

        BaseUpdatesGridGlanceWidget.DateLimit.toEpochMilli() shouldBeInRange (expected - 60_000L)..(expected + 60_000L)
    }

    @Test
    fun showsLockedNoticeWhenLocked() {
        WidgetInjekt.preferences.useAuthenticator.set(true)

        runWidgetSession(
            context = context,
            widget = UpdatesGridGlanceWidget(),
            receiver = UpdatesGridGlanceReceiver::class.java,
            appWidgetId = 1,
            size = SizeF(200f, 300f),
        ) {
            awaitUntil { it.matching(hasText(LOCKED_TEXT)) shouldBe 1 }
            // The clickable notice box, plus Glance's size box that mirrors its only child's modifier.
            awaitUntil { it.matching(opensAnActivity()) shouldBe 2 }
        }
        verify(exactly = 0) { getUpdates.subscribe(read = any(), after = any()) }
    }

    @Test
    fun showsCoverGridWhileUnlocked() {
        val updates = listOf(updateOf(1), updateOf(FAILING_MANGA_ID), updateOf(3))
        every { getUpdates.subscribe(read = false, after = any()) } returns flowOf(updates)

        runWidgetSession(
            context = context,
            widget = UpdatesGridGlanceWidget(),
            receiver = UpdatesGridGlanceReceiver::class.java,
            appWidgetId = 1,
            size = SizeF(200f, 300f),
        ) {
            awaitUntil { it.matching(opensAnActivity()) shouldBe 3 }
            awaitUntil { nodes ->
                nodes.matching(isResourceImage(R.drawable.appwidget_cover_error)) shouldBe 1
                nodes.matching(isProgressIndicator()) shouldBe 0
            }
        }
        requestedMangaIds shouldBe listOf(1L, FAILING_MANGA_ID, 3L)
        val limit = BaseUpdatesGridGlanceWidget.DateLimit.toEpochMilli()
        verify { getUpdates.subscribe(read = false, after = range(limit - 60_000L, limit)) }
    }

    @Test
    fun loadsCoversForLargestPlacement() {
        every { getUpdates.subscribe(read = false, after = any()) } returns flowOf((1L..4L).map { updateOf(it) })
        // Widget 2 is the larger placement (3 x 3 covers); the session below renders widget 1 (1 x 1).
        placeWidget(
            context = context,
            receiver = UpdatesGridGlanceReceiver::class.java,
            appWidgetId = 2,
            size = SizeF(200f, 300f),
        )

        runWidgetSession(
            context = context,
            widget = UpdatesGridGlanceWidget(),
            receiver = UpdatesGridGlanceReceiver::class.java,
            appWidgetId = 1,
            size = SizeF(100f, 100f),
        ) {
            awaitUntil { it.matching(opensAnActivity()) shouldBe 1 }
        }
        requestedMangaIds shouldBe listOf(1L, 2L, 3L, 4L)
    }
}
