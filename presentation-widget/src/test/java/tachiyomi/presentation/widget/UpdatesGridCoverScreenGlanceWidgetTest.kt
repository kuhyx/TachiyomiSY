package tachiyomi.presentation.widget

import android.content.Context
import android.util.SizeF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.appwidget.SizeMode
import androidx.glance.testing.unit.hasText
import androidx.glance.unit.ColorProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.interactor.GetUpdates

@RunWith(RobolectricTestRunner::class)
internal class UpdatesGridCoverScreenGlanceWidgetTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val getUpdates = mockk<GetUpdates>()
    private val requestedMangaIds = mutableListOf<Long>()

    @Before
    fun setUp() {
        WidgetInjekt.install(getUpdates)
        installImageLoader { request ->
            requestedMangaIds += (request.data as MangaCover).mangaId
            successFor(request, coverBitmap())
        }
    }

    @After
    fun tearDown() {
        resetImageLoader()
        WidgetInjekt.restore()
    }

    @Test
    fun usesWhiteOnCoverBackground() {
        val widget = UpdatesGridCoverScreenGlanceWidget()

        widget.sizeMode shouldBe SizeMode.Exact
        widget.foreground shouldBe ColorProvider(Color.White)
        widget.background.shouldBeInstanceOf<AndroidResourceImageProvider>().resId shouldBe
            R.drawable.appwidget_coverscreen_background
        widget.topPadding shouldBe 0.dp
        widget.bottomPadding shouldBe 24.dp
    }

    @Test
    fun receiverBuildsTheWidget() {
        UpdatesGridCoverScreenGlanceReceiver().glanceAppWidget.shouldBeInstanceOf<UpdatesGridCoverScreenGlanceWidget>()
    }

    @Test
    fun showsNoticeWithoutUpdates() {
        every { getUpdates.subscribe(read = false, after = any()) } returns flowOf(emptyList())

        runWidgetSession(
            context = context,
            widget = UpdatesGridCoverScreenGlanceWidget(),
            receiver = UpdatesGridCoverScreenGlanceReceiver::class.java,
            appWidgetId = 3,
            size = SizeF(200f, 300f),
        ) {
            awaitUntil { it.matching(hasText(NO_RECENT_TEXT)) shouldBe 1 }
            awaitUntil { it.matching(isProgressIndicator()) shouldBe 0 }
        }
        requestedMangaIds shouldBe emptyList()
    }

    @Test
    fun explicitArgumentsRenderTheGrid() {
        every { getUpdates.subscribe(read = false, after = any()) } returns flowOf(listOf(updateOf(1)))
        val widget = ExplicitArgsWidget()
        widget.topPadding shouldBe EXPLICIT_TOP_PADDING
        widget.bottomPadding shouldBe EXPLICIT_BOTTOM_PADDING

        runWidgetSession(
            context = context,
            widget = widget,
            receiver = ExplicitArgsReceiver::class.java,
            appWidgetId = 4,
            size = SizeF(200f, 300f),
        ) {
            awaitUntil { it.matching(opensAnActivity()) shouldBe 1 }
        }
        requestedMangaIds shouldBe listOf(1L)
    }

    @Test
    fun explicitArgsReceiverBuildsIt() {
        ExplicitArgsReceiver().glanceAppWidget.shouldBeInstanceOf<ExplicitArgsWidget>()
    }
}
