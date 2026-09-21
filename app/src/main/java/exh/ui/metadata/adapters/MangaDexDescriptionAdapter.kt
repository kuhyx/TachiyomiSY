package exh.ui.metadata.adapters

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DescriptionAdapterMdBinding
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.State
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.ui.metadata.adapters.MetadataUIUtil.bindDrawable

@Composable
internal fun MangaDexDescription(state: State.Success, openMetadataViewer: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { factoryContext ->
            DescriptionAdapterMdBinding.inflate(LayoutInflater.from(factoryContext)).root
        },
        update = {
            val meta = state.meta
            if (!(meta == null || meta !is MangaDexSearchMetadata)) {
                val binding = DescriptionAdapterMdBinding.bind(it)

                // todo
                val ratingFloat = meta.rating
                binding.ratingBar.rating = ratingFloat?.div(2F) ?: 0F
                binding.rating.text = ratingText(context, ratingFloat, outOfTen = ratingFloat)
                binding.rating.isVisible = ratingFloat != null
                binding.ratingBar.isVisible = ratingFloat != null

                binding.moreInfo.bindDrawable(context, R.drawable.ic_info_24dp)

                copyTextOnLongClick(context, binding.rating)

                binding.moreInfo.setOnClickListener {
                    openMetadataViewer()
                }
            }
        },
    )
}
