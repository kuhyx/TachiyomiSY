package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.readWithLongTap
import eu.kanade.tachiyomi.ui.reader.setting.readWithVolumeKeys
import eu.kanade.tachiyomi.ui.reader.setting.readWithVolumeKeysInverted
import eu.kanade.tachiyomi.ui.reader.setting.showNavigationOverlayNewUser
import eu.kanade.tachiyomi.ui.reader.setting.showNavigationOverlayOnStart
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.core.common.preference.Preference

// Common configuration for all viewers.
// Values of the pager/webtoon navigation-mode preferences (0 is the viewer's default layout).
private const val NAVIGATION_L = 1
private const val NAVIGATION_KINDLISH = 2
private const val NAVIGATION_EDGE = 3
private const val NAVIGATION_RIGHT_AND_LEFT = 4
private const val NAVIGATION_DISABLED = 5

private const val DEFAULT_DOUBLE_TAP_ANIM_MS = 500

internal abstract class ViewerConfig(readerPreferences: ReaderPreferences, private val scope: CoroutineScope) {

    var imagePropertyChangedListener: (() -> Unit)? = null

    var navigationModeChangedListener: (() -> Unit)? = null

    var tappingInverted = ReaderPreferences.TappingInvertMode.NONE
    var longTapEnabled = true
    var doubleTapAnimDuration = DEFAULT_DOUBLE_TAP_ANIM_MS
    var volumeKeysEnabled = false
    var volumeKeysInverted = false
    var alwaysShowChapterTransition = true
    var navigationMode = 0
        protected set

    var forceNavigationOverlay = false

    var navigationOverlayOnStart = false

    var dualPageSplit = false
        protected set

    var dualPageInvert = false
        protected set

    var dualPageRotateToFit = false
        protected set

    var dualPageRotateToFitInvert = false
        protected set

    abstract var navigator: ViewerNavigation
        protected set

    init {
        readerPreferences.readWithLongTap
            .register({ longTapEnabled = it })

        readerPreferences.doubleTapAnimSpeed
            .register({ doubleTapAnimDuration = it })

        readerPreferences.readWithVolumeKeys
            .register({ volumeKeysEnabled = it })

        readerPreferences.readWithVolumeKeysInverted
            .register({ volumeKeysInverted = it })

        readerPreferences.alwaysShowChapterTransition
            .register({ alwaysShowChapterTransition = it })

        forceNavigationOverlay = readerPreferences.showNavigationOverlayNewUser.get()
        if (forceNavigationOverlay) {
            readerPreferences.showNavigationOverlayNewUser.set(false)
        }

        readerPreferences.showNavigationOverlayOnStart
            .register({ navigationOverlayOnStart = it })
    }

    protected abstract fun defaultNavigation(): ViewerNavigation

    abstract fun updateNavigation(navigationMode: Int)

    /** The tap layout a navigation-mode preference selects; the viewer's default for 0 and unknown values. */
    protected fun navigationFor(navigationMode: Int): ViewerNavigation = when (navigationMode) {
        NAVIGATION_L -> LNavigation()
        NAVIGATION_KINDLISH -> KindlishNavigation()
        NAVIGATION_EDGE -> EdgeNavigation()
        NAVIGATION_RIGHT_AND_LEFT -> RightAndLeftNavigation()
        NAVIGATION_DISABLED -> DisabledNavigation()
        else -> defaultNavigation()
    }

    fun <T> Preference<T>.register(
        valueAssignment: (T) -> Unit,
        onChanged: (T) -> Unit = {},
    ) {
        changes()
            .onEach { valueAssignment(it) }
            .distinctUntilChanged()
            .onEach { onChanged(it) }
            .launchIn(scope)
    }
}
