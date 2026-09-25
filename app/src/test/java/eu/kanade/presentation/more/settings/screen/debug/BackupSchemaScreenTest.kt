package eu.kanade.presentation.more.settings.screen.debug

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import cafe.adriel.voyager.navigator.Navigator
import io.kotest.matchers.string.shouldContain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BackupSchemaScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsAndCopiesSchema() {
        compose.setContent { MaterialTheme { Navigator(BackupSchemaScreen()) } }
        compose.onNodeWithText("Backup file schema").assertExists()
        compose.onNodeWithContentDescription("Copy to clipboard").performClick()
        val clipboard = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip?.getItemAt(0)?.text.toString() shouldContain "message Backup"
    }
}
