package exh.ui.metadata.adapters

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DescriptionAdapterPuBinding
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.State
import exh.metadata.metadata.PururinSearchMetadata
import exh.ui.metadata.adapters.MetadataUIUtil.bindDrawable
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

@Composable
internal fun PururinDescription(state: State.Success, openMetadataViewer: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { factoryContext ->
            DescriptionAdapterPuBinding.inflate(LayoutInflater.from(factoryContext)).root
        },
        update = {
            val meta = state.meta
            if (meta is PururinSearchMetadata) {
                val binding = DescriptionAdapterPuBinding.bind(it)

                val genre = meta.tags.find { tag -> tag.namespace == PururinSearchMetadata.TAG_NAMESPACE_CATEGORY }
                binding.genre.bindGenre(context, genre?.name)

                binding.uploader.text = meta.uploaderDisp ?: meta.uploader.orEmpty()

                binding.size.text = meta.fileSize ?: context.stringResource(MR.strings.unknown)
                binding.size.bindDrawable(context, R.drawable.ic_outline_sd_card_24)

                val pages = meta.pages ?: 0
                binding.pages.text = context.pluralStringResource(SYMR.plurals.num_pages, pages, pages)
                binding.pages.bindDrawable(context, R.drawable.ic_baseline_menu_book_24)

                val ratingFloat = meta.averageRating?.toFloat()
                binding.ratingBar.rating = ratingFloat ?: 0F
                binding.rating.text = ratingText(context, ratingFloat, outOfTen = ratingFloat?.times(2))

                binding.moreInfo.bindDrawable(context, R.drawable.ic_info_24dp)

                copyTextOnLongClick(
                    context,
                    binding.genre,
                    binding.pages,
                    binding.rating,
                    binding.size,
                    binding.uploader,
                )

                binding.moreInfo.setOnClickListener {
                    openMetadataViewer()
                }
            }
        },
    )
}
