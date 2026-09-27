package eu.kanade.tachiyomi.ui.reader.viewer

import android.app.Application
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * A [ReaderPageImageView] in the app theme, over Koin holding its one preference, recording every callback
 * in [events]; Coil answers every request at once with [image], or with an error when it is null.
 */
@OptIn(DelicateCoilApi::class)
internal class PageImageRig {
    val app: Application = ApplicationProvider.getApplicationContext()
    val context: ContextThemeWrapper = ContextThemeWrapper(app, R.style.Theme_Tachiyomi)
    val base: BasePreferences = BasePreferences(app, MapPreferenceStore())
    val events: MutableList<String> = mutableListOf()
    var image: Image? = null

    fun start() {
        stopKoin()
        startKoin { modules(module { single { base } }) }
        val loader = mockk<ImageLoader>(relaxed = true)
        every { loader.enqueue(any()) } answers {
            val request = firstArg<ImageRequest>()
            val answer = image
            if (answer != null) {
                request.target?.onSuccess(answer)
            } else {
                request.listener?.onError(request, ErrorResult(null, request, IllegalStateException("no image")))
            }
            mockk(relaxed = true)
        }
        SingletonImageLoader.setUnsafe(loader)
    }

    fun stop() {
        SingletonImageLoader.reset()
        stopKoin()
    }

    fun view(webtoon: Boolean = false): ReaderPageImageView = ReaderPageImageView(context, isWebtoon = webtoon).apply {
        onImageLoaded = { events += "loaded" }
        onImageLoadError = { events += "error:${it?.message}" }
        onScaleChanged = { events += "scale:$it" }
        onViewClicked = { events += "click" }
    }
}

/** The image event listener the view gave its [SubsamplingScaleImageView]; the library keeps it private. */
internal fun SubsamplingScaleImageView.imageEvents(): SubsamplingScaleImageView.OnImageEventListener {
    val field = SubsamplingScaleImageView::class.java.getDeclaredField("onImageEventListener")
    field.isAccessible = true
    return field.get(this) as SubsamplingScaleImageView.OnImageEventListener
}

/** The state listener the view gave its [SubsamplingScaleImageView]. */
internal fun SubsamplingScaleImageView.stateEvents(): SubsamplingScaleImageView.OnStateChangedListener {
    val field = SubsamplingScaleImageView::class.java.getDeclaredField("onStateChangedListener")
    field.isAccessible = true
    return field.get(this) as SubsamplingScaleImageView.OnStateChangedListener
}
