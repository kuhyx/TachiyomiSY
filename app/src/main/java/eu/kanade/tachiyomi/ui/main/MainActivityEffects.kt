package eu.kanade.tachiyomi.ui.main

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.extension.api.ExtensionApi
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import eu.kanade.tachiyomi.ui.more.OnboardingScreen
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.model.getDownloadLink

@Composable
internal fun MainActivity.HandleOnNewIntent(context: Context, navigator: Navigator) {
    LaunchedEffect(Unit) {
        callbackFlow {
            val componentActivity = context as ComponentActivity
            val consumer = Consumer<Intent> { trySend(it) }
            componentActivity.addOnNewIntentListener(consumer)
            awaitClose { componentActivity.removeOnNewIntentListener(consumer) }
        }
            .collectLatest { handleIntentAction(it, navigator) }
    }
}

@Composable
internal fun MainActivity.CheckForUpdates() {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow

    // App updates
    LaunchedEffect(Unit) {
        if (BuildConfig.INCLUDE_UPDATER) {
            try {
                val result = AppUpdateChecker().checkForUpdate(context)
                if (result is GetApplicationRelease.Result.NewUpdate) {
                    val updateScreen = NewUpdateScreen(
                        versionName = result.release.version,
                        changelogInfo = result.release.info,
                        releaseLink = result.release.releaseLink,
                        downloadLink = result.release.getDownloadLink(),
                    )
                    navigator.push(updateScreen)
                }
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                logcat(LogPriority.ERROR, expected)
            }
        }
    }

    // Extensions updates
    LaunchedEffect(Unit) {
        try {
            ExtensionApi().checkForUpdates(context)
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
        }
    }
}

@Composable
internal fun MainActivity.ShowOnboarding() {
    val navigator = LocalNavigator.currentOrThrow

    LaunchedEffect(Unit) {
        if (!preferences.shownOnboardingFlow.get() && navigator.lastItem !is OnboardingScreen) {
            navigator.push(OnboardingScreen())
        }
    }
}
