package tachiyomi.presentation.widget

import android.content.Context
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Below Android 12 the launcher does not clip widget children, so the loader rounds the corners itself. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
internal class UpdatesCoverLoaderLegacyTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val requests = mutableListOf<ImageRequest>()

    @Before
    fun setUp() {
        WidgetInjekt.install()
        installImageLoader { request ->
            requests += request
            successFor(request, coverBitmap())
        }
    }

    @After
    fun tearDown() {
        resetImageLoader()
        WidgetInjekt.restore()
    }

    @Test
    fun roundsCornersBelowAndroid12() {
        val covers = runBlocking {
            UpdatesCoverLoader(context).load(listOf(updateOf(1)), rowCount = 1, columnCount = 1)
        }

        covers.single().second.shouldNotBeNull()
        requests.single().transformations.single().shouldBeInstanceOf<RoundedCornersTransformation>()
        requests.single().transformations.single().cacheKey shouldBe
            RoundedCornersTransformation(context.resources.getDimension(R.dimen.appwidget_inner_radius)).cacheKey
    }
}
