package tachiyomi.presentation.widget

import android.content.Context
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.manga.model.MangaCover

@RunWith(RobolectricTestRunner::class)
internal class UpdatesCoverLoaderTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val requests = mutableListOf<ImageRequest>()

    @Before
    fun setUp() {
        WidgetInjekt.install()
        installImageLoader { request ->
            requests += request
            val failing = (request.data as MangaCover).mangaId == FAILING_MANGA_ID
            if (failing) errorFor(request) else successFor(request, coverBitmap())
        }
    }

    @After
    fun tearDown() {
        resetImageLoader()
        WidgetInjekt.restore()
    }

    @Test
    fun loadsOneCoverPerMangaToGrid() {
        val updates = listOf(updateOf(1, chapterId = 11), updateOf(1, chapterId = 12), updateOf(2), updateOf(3))

        val covers = runBlocking { UpdatesCoverLoader(context).load(updates, rowCount = 1, columnCount = 2) }

        covers.map { it.first } shouldBe listOf(1L, 2L)
        covers.forEach { it.second.shouldNotBeNull() }
        requests.map { (it.data as MangaCover).mangaId } shouldBe listOf(1L, 2L)
    }

    @Test
    fun requestsExactSizeWithoutCache() {
        runBlocking { UpdatesCoverLoader(context).load(listOf(updateOf(5)), rowCount = 1, columnCount = 1) }

        val request = requests.single()
        request.memoryCachePolicy shouldBe CachePolicy.DISABLED
        request.precision shouldBe Precision.EXACT
        request.scale shouldBe Scale.FILL
        // Robolectric's system density is 1, so the 58 x 87 dp cover is requested at 58 x 87 px.
        runBlocking { request.sizeResolver.size() } shouldBe Size(58, 87)
        // Android 12 and later clip widget children, so no rounding is requested from Coil.
        request.transformations.shouldBeEmpty()
    }

    @Test
    fun describesTheCoverAsAFavourite() {
        runBlocking { UpdatesCoverLoader(context).load(listOf(updateOf(5)), rowCount = 1, columnCount = 1) }

        requests.single().data shouldBe MangaCover(
            mangaId = 5,
            sourceId = 7,
            isMangaFavorite = true,
            ogUrl = "https://covers.example/5.png",
            lastModified = 0,
        )
    }

    @Test
    fun pairsFailedLoadsWithNull() {
        val updates = listOf(updateOf(FAILING_MANGA_ID), updateOf(2))

        val covers = runBlocking { UpdatesCoverLoader(context).load(updates, rowCount = 2, columnCount = 2) }

        covers.map { it.first } shouldBe listOf(FAILING_MANGA_ID, 2L)
        covers[0].second.shouldBeNull()
        covers[1].second.shouldNotBeNull()
    }

    @Test
    fun loadsNothingForNoUpdates() {
        val covers = runBlocking { UpdatesCoverLoader(context).load(emptyList(), rowCount = 3, columnCount = 3) }

        covers.shouldBeEmpty()
        requests.shouldBeEmpty()
    }

    private companion object {
        const val FAILING_MANGA_ID = 9L
    }
}
