package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import android.content.ContentResolver
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.saver.ImageSaver
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.util.concurrent.atomic.AtomicBoolean

/** Covers that load as nothing, as a non-bitmap drawable or not at all, and a dismissed share error. */
@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
internal class MangaCoverEdgesTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    private val activity = controller.get()
    private val getManga = mockk<GetManga>()
    private val imageSaver = mockk<ImageSaver>()
    private val coverCache = mockk<CoverCache>(relaxed = true)
    private val saved: Uri = Uri.parse("content://covers/1")

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { getManga }
                    single { imageSaver }
                    single { coverCache }
                    single { mockk<UpdateManga>(relaxed = true) }
                    single { mockk<LocalCoverManager>(relaxed = true) }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
        coEvery { getManga.subscribe(1L) } returns MutableStateFlow(manga(favorite = true))
        every { imageSaver.save(any()) } returns saved
    }

    @After
    fun tearDown() {
        clearVoyagerScopes()
        stopKoin()
        SingletonImageLoader.reset()
        controller.pause().stop().destroy()
        shadowOf(Looper.getMainLooper()).idle()
        clearAllMocks()
    }

    // Every cover request answers with [image], or fails when it is null.
    private fun serve(image: Image?) {
        val loader = ImageLoader.Builder(app).components {
            add(
                Fetcher.Factory<Manga> { _, _, _ ->
                    Fetcher {
                        ImageFetchResult(
                            image = image ?: error("no cover"),
                            isSampled = false,
                            dataSource = DataSource.MEMORY,
                        )
                    }
                },
            )
        }.build()
        SingletonImageLoader.setUnsafe(loader)
    }

    private fun model(): MangaCoverScreenModel = MangaCoverScreenModel(1L).also { model ->
        eventually { model.state.value != null }
    }

    private fun MangaCoverScreenModel.snack(): String? = snackbarHostState.currentSnackbarData?.visuals?.message

    @Test
    fun failedLoadSavesNothing() {
        serve(null)
        val model = model()
        model.saveCover(activity)
        eventually { model.snack() == "Cover saved" }
        model.shareCover(activity)
        shadowOf(activity).nextStartedActivity shouldBe null
        verify(exactly = 0) { imageSaver.save(any()) }
    }

    @Test
    fun nonBitmapCoverSavesNothing() {
        serve(ColorDrawable(Color.RED).asImage())
        val model = model()
        model.saveCover(activity)
        eventually { model.snack() == "Cover saved" }
        verify(exactly = 0) { imageSaver.save(any()) }
    }

    @Test
    fun dismissedShareErrorEnds() {
        serve(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).asImage())
        val refusing = object : ContextWrapper(activity) {
            override fun startActivity(intent: Intent?) {
                error("no")
            }
        }
        val model = model()
        model.shareCover(refusing)
        eventually { model.snack() == "Error sharing cover" }
        model.snackbarHostState.currentSnackbarData?.dismiss()
        eventually { model.snack() == null }
    }

    @Test
    fun unreadableFileIsIgnored() {
        val opened = AtomicBoolean()
        val resolver = mockk<ContentResolver> {
            every { openInputStream(saved) } answers {
                opened.set(true)
                null
            }
        }
        val noStream = object : ContextWrapper(activity) {
            override fun getContentResolver(): ContentResolver = resolver
        }
        val model = model()
        model.editCover(noStream, saved)
        eventually { opened.get() }
        model.snack() shouldBe null
        verify(exactly = 0) { coverCache.setCustomCoverToCache(any(), any()) }
    }
}
