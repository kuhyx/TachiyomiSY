package eu.kanade.tachiyomi.ui.reader

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View.LAYER_TYPE_HARDWARE
import android.view.WindowManager
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.util.system.isNightMode
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import uy.kohesive.injekt.api.get
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.seconds

private const val CHANNEL_MAX = 255f
private const val GRAY_RED = 0x20
private const val GRAY_GREEN = 0x21
private const val GRAY_BLUE = 0x25
private const val THEME_WHITE = 0
private const val THEME_GRAY = 2
private const val THEME_AUTOMATIC = 3
private const val PERCENT = 100f
private const val MIN_BRIGHTNESS = 0.01f

/**
 * Reader-level configuration applied to the activity window and container: background colour,
 * display profile, screen-on, custom brightness, colour filters and double-page mode, each
 * following its preference for the activity's lifetime.
 */
internal class ReaderConfig(private val activity: ReaderActivity) {

    private val grayBackgroundColor = Color.rgb(GRAY_RED, GRAY_GREEN, GRAY_BLUE)

    /*
     * Initializes the reader subscriptions.
     */
    init {
        activity.readerPreferences.readerTheme.changes()
            .onEach { theme ->
                activity.binding.readerContainer.setBackgroundColor(
                    when (theme) {
                        THEME_WHITE -> Color.WHITE
                        THEME_GRAY -> grayBackgroundColor
                        THEME_AUTOMATIC -> automaticBackgroundColor()
                        else -> Color.BLACK
                    },
                )
            }
            .launchIn(activity.lifecycleScope)

        activity.preferences.displayProfile.changes()
            .onEach { setDisplayProfile(it) }
            .launchIn(activity.lifecycleScope)

        activity.readerPreferences.keepScreenOn.changes()
            .onEach(::setKeepScreenOn)
            .launchIn(activity.lifecycleScope)

        activity.readerPreferences.customBrightness.changes()
            .onEach(::setCustomBrightness)
            .launchIn(activity.lifecycleScope)

        combine(
            activity.readerPreferences.grayscale.changes(),
            activity.readerPreferences.invertedColors.changes(),
        ) { grayscale, invertedColors -> grayscale to invertedColors }
            .onEach { (grayscale, invertedColors) ->
                setLayerPaint(grayscale, invertedColors)
            }
            .launchIn(activity.lifecycleScope)

        combine(
            activity.readerPreferences.fullscreen.changes(),
            activity.readerPreferences.drawUnderCutout.changes(),
        ) { fullscreen, drawUnderCutout -> fullscreen to drawUnderCutout }
            .onEach { (fullscreen, drawUnderCutout) ->
                activity.updateViewerInset(fullscreen, drawUnderCutout)
            }
            .launchIn(activity.lifecycleScope)

        // SY -->
        activity.readerPreferences.pageLayout.changes()
            .drop(1)
            .onEach {
                activity.viewModel.setDoublePages(
                    (activity.viewModel.state.value.viewer as? PagerViewer)
                        ?.config
                        ?.doublePages
                        ?: false,
                )
            }
            .launchIn(activity.lifecycleScope)

        activity.readerPreferences.dualPageSplitPaged.changes()
            .drop(1)
            .onEach {
                if (!(activity.viewModel.state.value.viewer !is PagerViewer)) {
                    activity.reloadChapters(
                        !it &&
                            when (activity.readerPreferences.pageLayout.get()) {
                                PagerConfig.PageLayout.DOUBLE_PAGES -> true
                                PagerConfig.PageLayout.AUTOMATIC ->
                                    activity.resources.configuration.orientation ==
                                        Configuration.ORIENTATION_LANDSCAPE

                                else -> false
                            },
                        true,
                    )
                }
            }
            .launchIn(activity.lifecycleScope)
        // SY <--
    }

    private fun getCombinedPaint(grayscale: Boolean, invertedColors: Boolean): Paint {
        return Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply {
                    if (grayscale) {
                        setSaturation(0f)
                    }
                    if (invertedColors) {
                        postConcat(
                            ColorMatrix(
                                floatArrayOf(
                                    -1f, 0f, 0f, 0f, CHANNEL_MAX,
                                    0f, -1f, 0f, 0f, CHANNEL_MAX,
                                    0f, 0f, -1f, 0f, CHANNEL_MAX,
                                    0f, 0f, 0f, 1f, 0f,
                                ),
                            ),
                        )
                    }
                },
            )
        }
    }

    // Picks background color for [ReaderActivity] based on light/dark theme preference.
    private fun automaticBackgroundColor(): Int {
        return if (activity.baseContext.isNightMode()) {
            grayBackgroundColor
        } else {
            Color.WHITE
        }
    }

    // Sets the display profile to [path].
    private fun setDisplayProfile(path: String) {
        val file = UniFile.fromUri(activity.baseContext, path.toUri())
        if (file != null && file.exists()) {
            val inputStream = file.openInputStream()
            val outputStream = ByteArrayOutputStream()
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            val data = outputStream.toByteArray()
            SubsamplingScaleImageView.setDisplayProfile(data)
            TachiyomiImageDecoder.displayProfile = data
        }
    }

    // Sets the keep screen on mode according to [enabled].
    private fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Sets the custom brightness overlay according to [enabled].
    private fun setCustomBrightness(enabled: Boolean) {
        if (enabled) {
            activity.readerPreferences.customBrightnessValue.changes()
                .sample(0.1.seconds)
                .onEach(::setCustomBrightnessValue)
                .launchIn(activity.lifecycleScope)
        } else {
            setCustomBrightnessValue(0)
        }
    }

    // Sets the brightness of the screen. Range is [-75, 100].
    // From -75 to -1 a semi-transparent black view is overlaid with the minimum brightness.
    // From 1 to 100 it sets that value as brightness.
    // 0 sets system brightness and hides the overlay.
    private fun setCustomBrightnessValue(value: Int) {
        // Calculate and set reader brightness.
        val readerBrightness = when {
            value > 0 -> {
                value / PERCENT
            }

            value < 0 -> {
                MIN_BRIGHTNESS
            }

            else -> {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
        activity.window.attributes = activity.window.attributes.apply { screenBrightness = readerBrightness }

        activity.viewModel.setBrightnessOverlayValue(value)
    }

    private fun setLayerPaint(grayscale: Boolean, invertedColors: Boolean) {
        val paint = if (grayscale || invertedColors) getCombinedPaint(grayscale, invertedColors) else null
        activity.binding.viewerContainer.setLayerType(LAYER_TYPE_HARDWARE, paint)
    }
}
