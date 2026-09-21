package exh.ui.metadata.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DescriptionAdapterNhBinding
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.State
import eu.kanade.tachiyomi.util.system.copyToClipboard
import exh.metadata.MetadataUtil
import exh.metadata.metadata.NHentaiSearchMetadata
import exh.ui.metadata.adapters.MetadataUIUtil.bindDrawable
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
internal fun NHentaiDescription(state: State.Success, openMetadataViewer: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { factoryContext ->
            DescriptionAdapterNhBinding.inflate(LayoutInflater.from(factoryContext)).root
        },
        update = {
            val meta = state.meta
            if (!(meta == null || meta !is NHentaiSearchMetadata)) {
                val binding = DescriptionAdapterNhBinding.bind(it)
                binding.bindMetadata(context, meta)
                listOf(
                    binding.favorites,
                    binding.genre,
                    binding.id,
                    binding.pages,
                    binding.whenPosted,
                ).forEach { textView ->
                    textView.setOnLongClickListener {
                        context.copyToClipboard(textView.text.toString(), textView.text.toString())
                        true
                    }
                }
                binding.moreInfo.setOnClickListener {
                    openMetadataViewer()
                }
            }
        },
    )
}

private fun DescriptionAdapterNhBinding.bindMetadata(context: Context, meta: NHentaiSearchMetadata) {
    genre.text = meta.tags.filter {
        it.namespace == NHentaiSearchMetadata.NHENTAI_CATEGORIES_NAMESPACE
    }.let { tags ->
        if (tags.isNotEmpty()) tags.joinToString(transform = { it.name }) else null
    }.let { categoriesString ->
        categoriesString?.let { MetadataUIUtil.getGenreAndColour(context, it) }?.let {
            genre.setBackgroundColor(it.first)
            it.second
        } ?: categoriesString ?: context.stringResource(MR.strings.unknown)
    }
    meta.favoritesCount?.let {
        if (it != 0L) {
            favorites.text = NumberFormat.getIntegerInstance().format(it)
            favorites.bindDrawable(context, R.drawable.ic_book_24dp)
        }
    }
    whenPosted.text = MetadataUtil.EX_DATE_FORMAT.format(
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(meta.uploadDate ?: 0), ZoneId.systemDefault()),
    )
    pages.text = context.pluralStringResource(
        SYMR.plurals.num_pages,
        meta.pageImagePreviewUrls.size,
        meta.pageImagePreviewUrls.size,
    )
    pages.bindDrawable(context, R.drawable.ic_baseline_menu_book_24)
    @SuppressLint("SetTextI18n")
    id.text = "#" + (meta.nhId ?: 0)
    moreInfo.bindDrawable(context, R.drawable.ic_info_24dp)
}
