package eu.kanade.domain.source.model

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.getAppIconForSource
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.Source

@RunWith(RobolectricTestRunner::class)
internal class SourceTest {

    private val extensionManager = mockk<ExtensionManager>()

    @Before
    fun setUp() {
        startKoin { modules(module { single { extensionManager } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun iconIsNullWithoutAnExtension() {
        mockkStatic("eu.kanade.tachiyomi.extension.ExtensionManagerRegistryKt")
        every { extensionManager.getAppIconForSource(1) } returns null
        source(1).icon.shouldBeNull()
    }

    @Test
    fun iconComesFromTheExtension() {
        mockkStatic("androidx.core.graphics.drawable.DrawableKt")
        mockkStatic("eu.kanade.tachiyomi.extension.ExtensionManagerRegistryKt")
        val drawable = mockk<Drawable> {
            every { intrinsicWidth } returns 2
            every { intrinsicHeight } returns 2
        }
        every { drawable.toBitmap(any(), any(), any()) } returns Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        every { extensionManager.getAppIconForSource(2) } returns drawable
        source(2).icon.shouldNotBeNull()
    }

    private fun source(id: Long): Source =
        Source(id = id, lang = "en", name = "S", supportsLatest = false, isStub = false)
}
