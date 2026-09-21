package exh.ui.metadata.adapters

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DescriptionAdapterTsBinding
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.State
import exh.metadata.metadata.TsuminoSearchMetadata
import exh.ui.metadata.adapters.MetadataUIUtil.bindDrawable
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import java.text.NumberFormat
import java.util.Date

@Composable
internal fun TsuminoDescription(state: State.Success, openMetadataViewer: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { factoryContext ->
            DescriptionAdapterTsBinding.inflate(LayoutInflater.from(factoryContext)).root
        },
        update = {
            val meta = state.meta
            if (meta is TsuminoSearchMetadata) {
                val binding = DescriptionAdapterTsBinding.bind(it)

                binding.genre.bindGenre(context, meta.category)

                binding.favorites.text = NumberFormat.getIntegerInstance().format(meta.favorites ?: 0)
                binding.favorites.bindDrawable(context, R.drawable.ic_book_24dp)

                binding.whenPosted.text = TsuminoSearchMetadata.TSUMINO_DATE_FORMAT.format(Date(meta.uploadDate ?: 0))

                binding.uploader.text = meta.uploader ?: context.stringResource(MR.strings.unknown)

                val pages = meta.length ?: 0
                binding.pages.text = context.pluralStringResource(SYMR.plurals.num_pages, pages, pages)
                binding.pages.bindDrawable(context, R.drawable.ic_baseline_menu_book_24)

                val rating = meta.averageRating
                binding.ratingBar.rating = rating ?: 0F
                binding.rating.text = ratingText(context, rating, outOfTen = rating?.times(2))

                binding.moreInfo.bindDrawable(context, R.drawable.ic_info_24dp)

                copyTextOnLongClick(
                    context,
                    binding.favorites,
                    binding.genre,
                    binding.pages,
                    binding.rating,
                    binding.uploader,
                    binding.whenPosted,
                )

                binding.moreInfo.setOnClickListener {
                    openMetadataViewer()
                }
            }
        },
    )
}
