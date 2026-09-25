package eu.kanade.presentation.more.settings.screen.debug

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.profileinstaller.ProfileVerifier
import androidx.profileinstaller.ProfileVerifier.CompilationStatus
import androidx.test.core.app.ApplicationProvider
import cafe.adriel.voyager.navigator.LocalNavigator
import com.google.common.util.concurrent.Futures
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ProfileStatusTest(private val code: Int, private val label: String) {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        val store = MapPreferenceStore()
        stopKoin()
        startKoin {
            modules(
                module {
                    single { BasePreferences(ApplicationProvider.getApplicationContext(), store) }
                    single { UiPreferences(store) }
                },
            )
        }
        val status = mockk<CompilationStatus> { every { profileInstallResultCode } returns code }
        mockkStatic(ProfileVerifier::class)
        every { ProfileVerifier.getCompilationStatusAsync() } returns Futures.immediateFuture(status)
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    @Test
    fun statusIsLabelled() {
        compose.setContent {
            CompositionLocalProvider(LocalNavigator provides mockk(relaxed = true)) {
                MaterialTheme { DebugInfoScreen().Content() }
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    internal companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{1}")
        fun codes(): List<Array<Any>> = listOf(
            arrayOf(CompilationStatus.RESULT_CODE_NO_PROFILE_INSTALLED, "No profile installed"),
            arrayOf(CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE, "Compiled"),
            arrayOf(CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING, "Compiled non-matching"),
            failed(CompilationStatus.RESULT_CODE_ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ),
            failed(CompilationStatus.RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE),
            failed(CompilationStatus.RESULT_CODE_ERROR_PACKAGE_NAME_DOES_NOT_EXIST),
            arrayOf(CompilationStatus.RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION, "Not supported"),
            arrayOf(CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION, "Pending compilation"),
            arrayOf(12_345, "Unknown code 12345"),
        )

        private fun failed(code: Int): Array<Any> = arrayOf(code, "Error $code")
    }
}
