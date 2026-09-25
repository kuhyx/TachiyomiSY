package exh.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class SourceUtilTest {
    @get:Rule
    val compose = createComposeRule()

    private val initialized = MutableStateFlow(false)

    private fun install() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single<SourceManager> { mockk<SourceManager> { every { isInitialized } returns initialized } }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun readsTheSourceManagerFlag() {
        install()
        var loaded: Boolean? = null
        compose.setContent { loaded = ifSourcesLoaded() }
        compose.waitForIdle()
        loaded shouldBe false
        initialized.value = true
        compose.waitForIdle()
        loaded shouldBe true
    }
}
