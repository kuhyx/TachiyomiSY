package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.tachiyomi.util.storage.DiskUtil
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class StorageInfoTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() {
        mockkObject(DiskUtil)
        every { DiskUtil.getExternalStorages(any()) } returns listOf(folder.root)
        every { DiskUtil.getAvailableStorageSpace(folder.root) } returns 1_024L
        every { DiskUtil.getTotalStorageSpace(folder.root) } returns 4_096L
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun everyStorageListed() {
        compose.setContent { MaterialTheme { StorageInfo() } }
        compose.waitForIdle()
        compose.onNodeWithText(folder.root.absolutePath).assertExists()
        compose.onNodeWithText("Available: ", substring = true).assertExists()
    }

    @Test
    fun modifierIsApplied() {
        compose.setContent { MaterialTheme { StorageInfo(Modifier.testTag("storage")) } }
        compose.waitForIdle()
        compose.onNodeWithTag("storage").assertExists()
    }
}
