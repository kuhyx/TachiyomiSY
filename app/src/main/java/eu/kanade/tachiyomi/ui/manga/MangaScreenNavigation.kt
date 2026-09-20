package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.Navigator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import exh.pagepreview.PagePreviewScreen
import exh.recs.RecommendsScreen
import exh.source.MERGED_SOURCE_ID
import exh.ui.metadata.MetadataViewScreen
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal fun continueReading(context: Context, unreadChapter: Chapter?) {
    if (unreadChapter != null) openChapter(context, unreadChapter)
}

internal fun openChapter(context: Context, chapter: Chapter) {
    context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
}

internal fun openMangaInWebView(navigator: Navigator, manga: Manga?, source: Source?) {
    getMangaUrl(manga, source)?.let { url ->
        navigator.push(
            WebViewScreen(
                url = url,
                initialTitle = manga?.title,
                sourceId = source?.id,
            ),
        )
    }
}

internal fun openMetadataViewer(navigator: Navigator, manga: Manga) {
    navigator.push(MetadataViewScreen(manga.id, manga.source))
}

internal fun openMergedMangaWebview(context: Context, navigator: Navigator, mergedMangaData: MergedMangaData) {
    val sourceManager: SourceManager = Injekt.get()
    val mergedManga = mergedMangaData.manga.values.filterNot { it.source == MERGED_SOURCE_ID }
    val sources = mergedManga.map { sourceManager.getOrStub(it.source) }
    MaterialAlertDialogBuilder(context)
        .setTitle(MR.strings.action_open_in_web_view.getString(context))
        .setSingleChoiceItems(
            Array(mergedManga.size) { index -> sources[index].toString() },
            -1,
        ) { dialog, index ->
            dialog.dismiss()
            openMangaInWebView(navigator, mergedManga[index], sources[index] as? HttpSource)
        }
        .setNegativeButton(MR.strings.action_cancel.getString(context), null)
        .show()
}

internal fun openMorePagePreviews(navigator: Navigator, manga: Manga) {
    navigator.push(PagePreviewScreen(manga.id))
}

internal fun openPagePreview(context: Context, chapter: Chapter?, page: Int) {
    chapter ?: return
    context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id, page))
}

// EXH -->
internal fun openSmartSearch(navigator: Navigator, manga: Manga) {
    val smartSearchConfig = SourcesScreen.SmartSearchConfig(manga.title, manga.id)

    navigator.push(SourcesScreen(smartSearchConfig))
}

// AZ -->
internal fun openRecommends(navigator: Navigator, source: Source?, manga: Manga) {
    source ?: return
    RecommendsScreen.Args.SingleSourceManga(manga.id, source.id)
        .let(::RecommendsScreen)
        .let(navigator::push)
}
