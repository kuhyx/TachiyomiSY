package tachiyomi.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.SizeF
import androidx.compose.runtime.ProvidedValue
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BitmapImageProvider
import androidx.glance.EmittableImage
import androidx.glance.action.ActionModifier
import androidx.glance.action.StartActivityAction
import androidx.glance.appwidget.EmittableCircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.testing.GlanceNodeMatcher
import androidx.glance.testing.unit.MappedNode
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import io.mockk.coEvery
import io.mockk.mockk
import org.robolectric.Shadows.shadowOf

/** The English text of `MR.strings.appwidget_unavailable_locked`. */
internal const val LOCKED_TEXT: String = "Widget not available when app lock is enabled"

/** The English text of `MR.strings.information_no_recent`. */
internal const val NO_RECENT_TEXT: String = "No recent updates"

/**
 * Compose UI's `LocalContext` provided with [context]. `tachiyomi.presentation.core.i18n.stringResource`
 * reads that local, which Glance never provides, so the widget texts throw inside a real Glance
 * session (see `LockedWidgetTest`); tests that want to see the texts wrap the composable in
 * `CompositionLocalProvider(composeUiContext(context))`. Resolved reflectively because
 * `androidx.compose.ui:ui` is on this module's runtime classpath but only on the debug compile
 * classpath (`debugApi(ui-tooling)`), and the release unit tests compile the same sources.
 */
internal fun composeUiContext(context: Context): ProvidedValue<*> {
    val local = Class.forName("androidx.compose.ui.platform.AndroidCompositionLocals_androidKt")
        .getMethod("getLocalContext")
        .invoke(null)
    return local.javaClass.getMethod("provides", Any::class.java).invoke(local, context) as ProvidedValue<*>
}

/** Matches an image node drawn from exactly [bitmap]. */
internal fun isBitmapImage(bitmap: Bitmap): GlanceNodeMatcher<MappedNode> =
    GlanceNodeMatcher("is an image of $bitmap") { node ->
        val provider = (node.value.emittable as? EmittableImage)?.provider
        provider is BitmapImageProvider && provider.bitmap === bitmap
    }

/** Matches an image node drawn from drawable resource [resId]. */
internal fun isResourceImage(resId: Int): GlanceNodeMatcher<MappedNode> =
    GlanceNodeMatcher("is an image of resource $resId") { node ->
        val provider = (node.value.emittable as? EmittableImage)?.provider
        provider is AndroidResourceImageProvider && provider.resId == resId
    }

/** Matches the indeterminate spinner the grid shows while its covers load. */
internal fun isProgressIndicator(): GlanceNodeMatcher<MappedNode> =
    GlanceNodeMatcher("is a progress indicator") { it.value.emittable is EmittableCircularProgressIndicator }

/** Matches a node whose click starts an activity. */
internal fun opensAnActivity(): GlanceNodeMatcher<MappedNode> =
    GlanceNodeMatcher("starts an activity on click") { node ->
        node.value.emittable.modifier.any { it is ActionModifier && it.action is StartActivityAction }
    }

/**
 * Binds app widget [appWidgetId] to [receiver] with [size] as its only size option, and lists every
 * widget receiver of the module as installed so `GlanceAppWidgetManager` can map providers to
 * receivers. Binding (rather than `createWidget`) keeps Robolectric from delivering broadcasts to a
 * receiver instance, whose Glance session would need a WorkManager the test JVM does not have.
 */
internal fun placeWidget(
    context: Context,
    receiver: Class<out GlanceAppWidgetReceiver>,
    appWidgetId: Int,
    size: SizeF,
) {
    val manager = AppWidgetManager.getInstance(context)
    val shadow = shadowOf(manager)
    WIDGET_RECEIVERS.forEach { shadow.addInstalledProvider(providerInfo(context, it)) }
    shadow.addBoundWidget(appWidgetId, providerInfo(context, receiver))
    val options = Bundle().apply {
        putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, arrayListOf(size))
    }
    manager.updateAppWidgetOptions(appWidgetId, options)
}

private val WIDGET_RECEIVERS: List<Class<out GlanceAppWidgetReceiver>> = listOf(
    UpdatesGridGlanceReceiver::class.java,
    UpdatesGridCoverScreenGlanceReceiver::class.java,
    ExplicitArgsReceiver::class.java,
)

private fun providerInfo(context: Context, receiver: Class<out GlanceAppWidgetReceiver>): AppWidgetProviderInfo =
    AppWidgetProviderInfo().apply { provider = ComponentName(context, receiver) }

/** Installs a mock Coil loader that answers every request with [answer]; undo with [resetImageLoader]. */
@OptIn(DelicateCoilApi::class)
internal fun installImageLoader(answer: (ImageRequest) -> ImageResult): ImageLoader {
    val loader = mockk<ImageLoader>()
    coEvery { loader.execute(any()) } answers { answer(firstArg()) }
    SingletonImageLoader.setUnsafe(loader)
    return loader
}

/** Drops the loader installed by [installImageLoader]. */
@OptIn(DelicateCoilApi::class)
internal fun resetImageLoader() {
    SingletonImageLoader.reset()
}

/** A successful load of [bitmap] for [request]. */
internal fun successFor(request: ImageRequest, bitmap: Bitmap): ImageResult =
    SuccessResult(image = bitmap.asImage(), request = request, dataSource = DataSource.MEMORY)

/** A failed load for [request], which carries no image. */
internal fun errorFor(request: ImageRequest): ImageResult =
    ErrorResult(image = null, request = request, throwable = IllegalStateException("no cover"))

/** A one-pixel cover. */
internal fun coverBitmap(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
