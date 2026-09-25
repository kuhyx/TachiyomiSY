package eu.kanade.tachiyomi.ui.download

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import androidx.activity.ComponentActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import eu.kanade.tachiyomi.source.online.HttpSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.robolectric.Robolectric
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

/** A mocked download manager over a real queue, and the list the queue screen renders it into. */
internal class DownloadHarness {
    val context: Context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_Tachiyomi)
    val queue: MutableStateFlow<List<Download>> = MutableStateFlow(emptyList())
    val running: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** An activity to attach lists to, so posted work (the row menu) runs. */
    val host: ComponentActivity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    val manager: DownloadManager = mockk(relaxed = true) {
        every { queueState } returns queue
        every { isDownloaderRunning } returns running
        every { statusFlow() } returns emptyFlow()
        every { progressFlow() } returns emptyFlow()
    }

    fun model(): DownloadQueueScreenModel = DownloadQueueScreenModel(manager)

    /** Binds [model] to an inflated, laid-out queue list showing [headers], as the screen's factory does. */
    fun attach(model: DownloadQueueScreenModel, headers: List<DownloadHeaderItem>): DownloadListBinding {
        val binding = DownloadListBinding.inflate(LayoutInflater.from(context))
        val adapter = DownloadAdapter(model.listener)
        model.controllerBinding = binding
        model.adapter = adapter
        binding.root.adapter = adapter
        binding.root.layoutManager = LinearLayoutManager(context)
        adapter.updateDataSet(headers)
        host.setContentView(binding.root)
        layout(binding.root)
        return binding
    }

    fun layout(view: View) {
        view.measure(
            MeasureSpec.makeMeasureSpec(WIDTH, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(HEIGHT, MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, WIDTH, HEIGHT)
    }
}

private val sources = HashMap<Long, HttpSource>()

/** The one mocked source per id, so downloads of the same source group together. */
internal fun httpSource(id: Long): HttpSource = sources.getOrPut(id) {
    val source = mockk<HttpSource>()
    every { source.id } returns id
    every { source.name } returns "S$id"
    source
}

internal fun download(
    chapterId: Long,
    source: HttpSource = httpSource(1),
    mangaId: Long = 1,
    dateUpload: Long = chapterId,
): Download = Download(
    source = source,
    manga = Manga.create().copy(id = mangaId, ogTitle = "Manga $mangaId"),
    chapter = Chapter.create().copy(id = chapterId, name = "Chapter $chapterId", dateUpload = dateUpload),
)

/** One header per source with its downloads as sub-items, as the model builds them. */
internal fun headers(vararg downloads: Download): List<DownloadHeaderItem> =
    downloads.groupBy { it.source }.map { (source, items) ->
        DownloadHeaderItem(source.id, source.name, items.size).apply {
            addSubItems(0, items.map { DownloadItem(it, this) })
        }
    }

private const val WIDTH = 1080
private const val HEIGHT = 1920
