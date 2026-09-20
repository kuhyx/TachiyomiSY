package eu.kanade.tachiyomi.ui.main

import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import eu.kanade.presentation.components.AppStateBanners
import eu.kanade.presentation.components.DownloadedOnlyBannerBackgroundColor
import eu.kanade.presentation.components.IncognitoModeBannerBackgroundColor
import eu.kanade.presentation.components.IndexingBannerBackgroundColor
import eu.kanade.presentation.more.settings.screen.ConfigureExhDialog
import eu.kanade.presentation.more.settings.screen.about.WhatsNewDialog
import eu.kanade.presentation.util.DefaultScreenTransition
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.isBenchmarkBuildType
import eu.kanade.tachiyomi.util.system.isNavigationBarNeedsScrim
import exh.debug.DebugToggles
import exh.eh.EHentaiUpdateWorker
import exh.log.DebugModeOverlay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.collectAsState as collectFlowAsState

private const val BANNER_ALPHA = 0.8f

// Above this luminance the status bar background is light and wants dark icons.
private const val LIGHT_STATUS_BAR_LUMINANCE = 0.5

/** The activity's whole Compose tree: system bars, the root navigator with its banners, and the one-shot dialogs. */
@Composable
internal fun MainActivity.MainContent(isLaunch: Boolean, didMigration: Boolean, hasDebugOverlay: Boolean) {
    var incognito by remember { mutableStateOf(getIncognitoState.await(null)) }
    val downloadOnly by preferences.downloadedOnly.collectAsState()
    val indexing by downloadCache.isInitializing.collectFlowAsState()

    val statusBarBackgroundColor = when {
        indexing -> IndexingBannerBackgroundColor
        downloadOnly -> DownloadedOnlyBannerBackgroundColor
        incognito -> IncognitoModeBannerBackgroundColor
        else -> MaterialTheme.colorScheme.surface
    }
    SystemBarsEffect(statusBarBackgroundColor.luminance() > LIGHT_STATUS_BAR_LUMINANCE)

    Navigator(
        screen = HomeScreen,
        disposeBehavior = NavigatorDisposeBehavior(disposeNestedNavigators = false, disposeSteps = true),
    ) { navigator ->
        LaunchedEffect(navigator) {
            this@MainContent.navigator = navigator
            if (isLaunch) onFirstLaunch(navigator)
        }
        LaunchedEffect(navigator.lastItem) {
            (navigator.lastItem as? BrowseSourceScreen)?.sourceId
                .let(getIncognitoState::subscribe)
                .collectLatest { incognito = it }
        }
        MainScaffold(navigator, downloadOnly = downloadOnly, incognito = incognito, indexing = indexing)
        IncognitoOffPopsSourceScreens(navigator)
        HandleOnNewIntent(context = LocalContext.current, navigator = navigator)
        if (!isBenchmarkBuildType) {
            CheckForUpdates()
            ShowOnboarding()
        }
    }

    // SY -->
    if (hasDebugOverlay) {
        val isDebugOverlayEnabled by remember { DebugToggles.ENABLE_DEBUG_OVERLAY.asPref(lifecycleScope) }
        if (isDebugOverlayEnabled) {
            DebugModeOverlay()
        }
    }
    // SY <--
    var showChangelog by remember { mutableStateOf(didMigration && !BuildConfig.DEBUG && !isBenchmarkBuildType) }
    if (showChangelog) {
        // SY -->
        WhatsNewDialog(onDismissRequest = { showChangelog = false })
        // SY <--
    }
    // SY -->
    ConfigureExhDialog(run = runExhConfigureDialog, onRunning = { runExhConfigureDialog = false })
    // SY <--
}

// Draws edge-to-edge and keeps the transparent system bars' icon colour readable over the status banner.
@Composable
private fun MainActivity.SystemBarsEffect(lightStatusBar: Boolean) {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(isSystemInDarkTheme, lightStatusBar) {
        val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
        val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(
            statusBarStyle = if (lightStatusBar) lightStyle else darkStyle,
            navigationBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
        )
    }
}

private fun MainActivity.onFirstLaunch(navigator: Navigator) {
    // Set start screen
    handleIntentAction(intent, navigator)
    // Reset Incognito Mode on relaunch
    preferences.incognitoMode.set(false)
    // SY -->
    initWhenIdle {
        // Upload settings
        if (exhPreferences.enableExhentai.get() && exhPreferences.exhShowSettingsUploadWarning.get()) {
            runExhConfigureDialog = true
        }
        // Scheduler uploader job if required
        EHentaiUpdateWorker.scheduleBackground(this)
    }
    // SY <--
}

@Composable
private fun MainScaffold(navigator: Navigator, downloadOnly: Boolean, incognito: Boolean, indexing: Boolean) {
    val context = LocalContext.current
    val scaffoldInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)
    Scaffold(
        topBar = {
            AppStateBanners(
                downloadedOnlyMode = downloadOnly,
                incognitoMode = incognito,
                indexing = indexing,
                modifier = Modifier.windowInsetsPadding(scaffoldInsets),
            )
        },
        contentWindowInsets = scaffoldInsets,
    ) { contentPadding ->
        // Consume insets already used by app state banners
        Box {
            // Shows current screen
            DefaultScreenTransition(
                navigator = navigator,
                modifier = Modifier
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
            )
            // Draw navigation bar scrim when needed
            if (remember { context.isNavigationBarNeedsScrim() }) {
                Spacer(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .alpha(BANNER_ALPHA)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
    }
}

// Pop source-related screens when incognito mode is turned off.
@Composable
private fun MainActivity.IncognitoOffPopsSourceScreens(navigator: Navigator) {
    LaunchedEffect(Unit) {
        preferences.incognitoMode.changes()
            .drop(1)
            .filter { !it }
            .onEach {
                val currentScreen = navigator.lastItem
                if (currentScreen is BrowseSourceScreen || (currentScreen is MangaScreen && currentScreen.fromSource)) {
                    navigator.popUntilRoot()
                }
            }
            .launchIn(this)
    }
}
