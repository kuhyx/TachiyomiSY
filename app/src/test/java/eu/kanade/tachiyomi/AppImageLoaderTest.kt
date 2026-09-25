package eu.kanade.tachiyomi

import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.di.startAppGraph
import eu.kanade.tachiyomi.di.stopAppGraph
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppImageLoaderTest {
    private val client = OkHttpClient()
    private val preferences = NetworkPreferences(MapPreferenceStore())
    private val app = attachedApp()

    @Before
    fun setUp() {
        val network = mockk<NetworkHelper> { every { client } returns this@AppImageLoaderTest.client }
        startAppGraph(
            app,
            module {
                single { preferences }
                single { network }
            },
        )
    }

    @After
    fun tearDown() {
        stopAppGraph()
    }

    @Test
    fun quietLoaderSharesTheClient() {
        val loader = app.newImageLoader(app)
        val factory = loader.components.fetcherFactories
            .map { it.first }
            .filterIsInstance<MangaCoverFetcher.MangaCoverFactory>()
            .single()
        val field = MangaCoverFetcher.MangaCoverFactory::class.java.getDeclaredField("callFactoryLazy")
        field.isAccessible = true
        (field.get(factory) as Lazy<*>).value shouldBeSameInstanceAs client
    }

    @Test
    fun verboseLoaderLogs() {
        preferences.verboseLogging.set(true)
        app.newImageLoader(app).components.fetcherFactories.isNotEmpty() shouldBe true
    }
}
