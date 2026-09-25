package eu.kanade.presentation.util

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class PermissionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun resumeReadsThePermission() {
        shadowOf(context.packageManager).setCanRequestPackageInstalls(true)
        var shown by mutableStateOf(true)
        compose.setContent {
            if (shown) Text("granted ${rememberInstallPermissionState()}")
        }
        compose.onNodeWithText("granted true").assertExists()
        shown = false
        compose.waitForIdle()
        compose.onNodeWithText("granted true").assertDoesNotExist()
    }

    @Test
    fun deniedWhenPackageManagerSaysNo() {
        shadowOf(context.packageManager).setCanRequestPackageInstalls(false)
        compose.setContent { Text("granted ${rememberInstallPermissionState(initialValue = true)}") }
        compose.onNodeWithText("granted false").assertExists()
    }
}
