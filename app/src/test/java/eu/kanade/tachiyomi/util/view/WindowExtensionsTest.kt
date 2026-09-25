package eu.kanade.tachiyomi.util.view

import android.view.Window
import android.view.WindowManager
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class WindowExtensionsTest {

    @Test
    fun secureScreenSetsAndClearsThe() {
        val window = mockk<Window>(relaxed = true)
        window.setSecureScreen(true)
        verify(exactly = 1) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        window.setSecureScreen(false)
        verify(exactly = 1) { window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
