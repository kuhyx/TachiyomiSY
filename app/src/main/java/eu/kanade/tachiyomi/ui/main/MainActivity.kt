package eu.kanade.tachiyomi.ui.main

import android.app.assist.AssistContent
import android.os.Bundle
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.system.isBenchmarkBuildType
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import eu.kanade.tachiyomi.util.view.setComposeContent
import exh.SY_DEBUG_VERSION
import exh.source.BlacklistedSources
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.ExhPreferences
import mihon.core.migration.Migrator
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.injectLazy
import java.util.LinkedList

internal class MainActivity : BaseActivity() {

    private val libraryPreferences: LibraryPreferences by injectLazy()
    internal val preferences: BasePreferences by injectLazy()

    // SY -->
    internal val exhPreferences: ExhPreferences by injectLazy()
    // SY <--

    internal val downloadCache: DownloadCache by injectLazy()
    private val chapterCache: ChapterCache by injectLazy()

    internal val getIncognitoState: GetIncognitoState by injectLazy()

    // To be checked by splash screen. If true then splash screen will be removed.
    var ready = false

    internal var navigator: Navigator? = null

    init {
        registerSecureActivity(this)
    }

    // SY -->
    // Idle-until-urgent
    private var firstPaint = false
    private val iuuQueue = LinkedList<() -> Unit>()

    internal var runExhConfigureDialog by mutableStateOf(false)

    internal fun initWhenIdle(task: () -> Unit) {
        // Avoid sync issues by enforcing main thread
        check(Looper.myLooper() == Looper.getMainLooper()) { "Can only be called on main thread!" }

        if (firstPaint) {
            task()
        } else {
            iuuQueue += task
        }
    }

    // SY <--

    override fun onCreate(savedInstanceState: Bundle?) {
        val isLaunch = savedInstanceState == null

        // Prevent splash screen showing up on configuration changes
        val splashScreen = if (isLaunch) installSplashScreen() else null

        super.onCreate(savedInstanceState)

        val didMigration = if (isLaunch) {
            addAnalytics()
            Migrator.awaitAndRelease()
        } else {
            false
        }

        // Do not let the launcher create a new activity http://stackoverflow.com/questions/16283079
        if (!isTaskRoot) {
            finish()
            return
        }

        // SY -->
        @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
        val hasDebugOverlay = (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "releaseTest") && !isBenchmarkBuildType
        // SY <--

        setComposeContent { MainContent(isLaunch, didMigration, hasDebugOverlay) }

        val startTime = System.currentTimeMillis()
        splashScreen?.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            elapsed <= SPLASH_MIN_DURATION || (!ready && elapsed <= SPLASH_MAX_DURATION)
        }
        setSplashScreenExitAnimation(splashScreen)

        if (isLaunch && libraryPreferences.autoClearChapterCache.get()) {
            lifecycleScope.launchIO {
                chapterCache.clear()
            }
        }

        // SY -->
        if (!exhPreferences.isHentaiEnabled.get()) {
            BlacklistedSources.HIDDEN_SOURCES += EH_SOURCE_ID
            BlacklistedSources.HIDDEN_SOURCES += EXH_SOURCE_ID
        }
        // SY -->
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        when (val screen = navigator?.lastItem) {
            is AssistContentScreen -> {
                screen.onProvideAssistUrl()?.let { outContent.webUri = it.toUri() }
            }
        }
    }

    // SY -->
    private fun addAnalytics() {
        if (!BuildConfig.DEBUG && isPreviewBuildType) {
            Firebase.analytics.setUserProperty("preview_version", SY_DEBUG_VERSION)
        }
    }
    // SY <--

    companion object {
        const val INTENT_SEARCH = "eu.kanade.tachiyomi.SEARCH"
        const val INTENT_SEARCH_QUERY = "query"
        const val INTENT_SEARCH_FILTER = "filter"
    }
}

// Splash screen
private const val SPLASH_MIN_DURATION = 500 // ms
private const val SPLASH_MAX_DURATION = 5000 // ms
