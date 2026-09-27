package eu.kanade.tachiyomi.ui.main

import androidx.compose.runtime.Composable
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen

/** An empty screen that offers [url] to the assistant. */
internal class AssistingScreen(private val url: String?) : Screen(), AssistContentScreen {
    override fun onProvideAssistUrl(): String? = url

    @Composable
    override fun Content() = Unit
}
