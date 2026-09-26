package exh.ui.metadata.adapters

import android.view.View
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import eu.kanade.tachiyomi.ui.manga.PagePreviewState
import exh.metadata.metadata.RaisedSearchMetadata
import io.mockk.mockk
import tachiyomi.domain.manga.model.Manga

/** A loaded manga screen state carrying [meta] and nothing else of note. */
internal fun successState(meta: RaisedSearchMetadata?): MangaScreenModel.State.Success =
    MangaScreenModel.State.Success(
        manga = Manga.create(),
        source = mockk(),
        isFromSource = false,
        chapters = emptyList(),
        availableScanlators = emptySet(),
        excludedScanlators = emptySet(),
        meta = meta,
        mergedData = null,
        showRecommendationsInOverflow = false,
        showMergeInOverflow = false,
        showMergeWithAnother = false,
        pagePreviewsState = PagePreviewState.Loading,
        alwaysShowReadingProgress = false,
        previewsRowCount = 0,
    )

/** Composes one description adapter and exposes the view tree it inflated. */
internal class DescriptionHost(private val compose: ComposeContentTestRule) {
    private var host: View? = null

    /** How often the adapter asked for the metadata viewer. */
    var viewerOpened: Int = 0

    fun show(content: @Composable (open: () -> Unit) -> Unit) {
        compose.setContent {
            host = LocalView.current
            MaterialTheme { content { viewerOpened++ } }
        }
        compose.waitForIdle()
    }

    fun text(id: Int): String = checkNotNull(host).findViewById<TextView>(id).text.toString()

    fun view(id: Int): View = checkNotNull(host).findViewById(id)

    /** Long-presses every view in [ids] and taps "more info". */
    fun pressAll(vararg ids: Int) {
        ids.forEach { view(it).performLongClick() }
        view(R.id.more_info).performClick()
        compose.waitForIdle()
    }
}
