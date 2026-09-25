package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.saver.ImageSaver
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.image.LocalCoverManager
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
internal class MangaCoverScreenModelTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val manga = MutableStateFlow<Manga?>(manga(favorite = true))
    private val getManga = mockk<GetManga> { coEvery { subscribe(1L) } returns manga }
    private val imageSaver = mockk<ImageSaver>()
    private val coverCache = mockk<CoverCache>(relaxed = true)
    private val updateManga = mockk<UpdateManga>(relaxed = true)
    private val coverManager = mockk<LocalCoverManager>(relaxed = true)
    private val saved: Uri = Uri.parse("content://covers/1")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startKoin {
            modules(
                module {
                    single { getManga }
                    single { imageSaver }
                    single { coverCache }
                    single { updateManga }
                    single { coverManager }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val loader = ImageLoader.Builder(app).components {
            add(
                Fetcher.Factory<Manga> { _, _, _ ->
                    Fetcher { ImageFetchResult(bitmap.asImage(), isSampled = false, dataSource = DataSource.MEMORY) }
                },
            )
        }.build()
        SingletonImageLoader.setUnsafe(loader)
    }

    @After
    fun tearDown() {
        stopKoin()
        SingletonImageLoader.reset()
        Dispatchers.resetMain()
        clearVoyagerScopes()
    }

    private fun model(): MangaCoverScreenModel = MangaCoverScreenModel(1L).also { model ->
        eventually { model.state.value != null }
    }

    private fun MangaCoverScreenModel.snack(): String? = snackbarHostState.currentSnackbarData?.visuals?.message

    @Test
    fun savesTheCover() {
        every { imageSaver.save(any()) } returns saved
        val model = model()
        model.saveCover(activity)
        eventually { model.snack() == "Cover saved" }
        verify { imageSaver.save(any()) }
    }

    @Test
    fun saveFailureIsReported() {
        every { imageSaver.save(any()) } throws IllegalStateException("disk")
        val model = model()
        model.saveCover(activity)
        eventually { model.snack() == "Error saving cover" }
    }

    @Test
    fun sharesTheSavedCover() {
        every { imageSaver.save(any()) } returns saved
        model().shareCover(activity)
        eventually { shadowOf(activity).peekNextStartedActivity() != null }
        shadowOf(activity).nextStartedActivity.action shouldBe Intent.ACTION_CHOOSER
    }

    @Test
    fun shareFailureIsReported() {
        every { imageSaver.save(any()) } returns saved
        val refusing = object : ContextWrapper(activity) {
            override fun startActivity(intent: Intent?): Unit = throw IllegalStateException("no")
        }
        val model = model()
        model.shareCover(refusing)
        eventually { model.snack() == "Error sharing cover" }
    }

    @Test
    fun missingEntryDoesNothing() {
        manga.value = null
        val model = MangaCoverScreenModel(1L)
        model.shareCover(activity)
        model.saveCover(activity)
        model.editCover(activity, saved)
        model.deleteCustomCover(activity)
        eventually { model.snack() == "Cover saved" }
        shadowOf(activity).nextStartedActivity shouldBe null
    }

    @Test
    fun editsTheCover() {
        val stream = ByteArrayInputStream(byteArrayOf(1))
        val resolver = shadowOf(activity.contentResolver)
        resolver.registerInputStream(saved, stream)
        val model = model()
        model.editCover(activity, saved)
        eventually { model.snack() == "Cover updated" }
        verify { coverCache.setCustomCoverToCache(any(), any()) }
    }

    @Test
    fun editFailureIsReported() {
        shadowOf(activity.contentResolver).registerInputStream(saved, ByteArrayInputStream(byteArrayOf(1)))
        every { coverCache.setCustomCoverToCache(any(), any()) } throws IllegalStateException("io")
        val model = model()
        model.editCover(activity, saved)
        eventually { model.snack() == "Failed to update cover" }
    }

    @Test
    fun deletesTheCustomCover() {
        val model = model()
        model.deleteCustomCover(activity)
        eventually { model.snack() == "Cover updated" }
        coVerify { updateManga.awaitUpdateCoverLastModified(1L) }
        model.snackbarHostState.currentSnackbarData?.dismiss()
        every { coverCache.deleteCustomCover(1L) } throws IllegalStateException("io")
        model.deleteCustomCover(activity)
        eventually { model.snack() == "Failed to update cover" }
    }
}
