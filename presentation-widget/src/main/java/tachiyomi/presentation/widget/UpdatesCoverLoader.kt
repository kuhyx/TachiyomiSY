package tachiyomi.presentation.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import coil3.annotation.ExperimentalCoilApi
import coil3.asDrawable
import coil3.executeBlocking
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Scale
import coil3.transform.RoundedCornersTransformation
import eu.kanade.tachiyomi.util.system.dpToPx
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.presentation.widget.components.CoverHeight
import tachiyomi.presentation.widget.components.CoverWidth

/** Loads the cover bitmaps the updates grid shows, one per distinct manga, at cover size. */
internal class UpdatesCoverLoader(private val context: Context) {

    /**
     * The first `rowCount * columnCount` distinct manga of [updates] paired with their cover,
     * `null` where Coil could not load one. Corners are rounded here below Android 12, where
     * the launcher does not clip widget children.
     */
    @OptIn(ExperimentalCoilApi::class)
    suspend fun load(
        updates: List<UpdatesWithRelations>,
        rowCount: Int,
        columnCount: Int,
    ): List<Pair<Long, Bitmap?>> {
        // Resize to cover size
        val widthPx = CoverWidth.value.toInt().dpToPx
        val heightPx = CoverHeight.value.toInt().dpToPx
        val roundPx = context.resources.getDimension(R.dimen.appwidget_inner_radius)
        return withIOContext {
            updates
                .distinctBy { it.mangaId }
                .take(rowCount * columnCount)
                .map { updatesView ->
                    val request = ImageRequest.Builder(context)
                        .data(updatesView.toMangaCover())
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .precision(Precision.EXACT)
                        .size(widthPx, heightPx)
                        .scale(Scale.FILL)
                        .let {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                it.transformations(RoundedCornersTransformation(roundPx))
                            } else {
                                it // Handled by system
                            }
                        }
                        .build()
                    val bitmap = context.imageLoader.executeBlocking(request)
                        .image
                        ?.asDrawable(context.resources)
                        ?.toBitmap()
                    Pair(updatesView.mangaId, bitmap)
                }
        }
    }

    private fun UpdatesWithRelations.toMangaCover(): MangaCover = MangaCover(
        mangaId = mangaId,
        sourceId = sourceId,
        isMangaFavorite = true,
        ogUrl = coverData.url,
        lastModified = coverData.lastModified,
    )
}
