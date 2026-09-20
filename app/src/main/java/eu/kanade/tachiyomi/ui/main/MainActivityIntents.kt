package eu.kanade.tachiyomi.ui.main

import android.app.SearchManager
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.deeplink.DeepLinkScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.coroutines.launch
import tachiyomi.core.common.Constants

// The Google-specific search intent (triggered by saying or typing "search *query* on *Tachiyomi*"
// in Google Search/Google Assistant); Android's own is Intent.ACTION_SEARCH.
private const val GOOGLE_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION"

/** What an incoming intent asks of the home screen. */
private sealed interface IntentTarget {
    data class Tab(val tab: HomeScreen.Tab, val popToRoot: Boolean = false) : IntentTarget
    data class Push(val screen: Screen) : IntentTarget
    object Handled : IntentTarget
    object Unhandled : IntentTarget
}

// The shortcut actions that only switch the home tab.
private val shortcutTabs: Map<String, () -> HomeScreen.Tab> = mapOf(
    Constants.SHORTCUT_LIBRARY to { HomeScreen.Tab.Library() },
    Constants.SHORTCUT_UPDATES to { HomeScreen.Tab.Updates },
    Constants.SHORTCUT_HISTORY to { HomeScreen.Tab.History },
    Constants.SHORTCUT_SOURCES to { HomeScreen.Tab.Browse(false) },
    Constants.SHORTCUT_EXTENSIONS to { HomeScreen.Tab.Browse(true) },
)

/** Routes a launch/new intent to its screen; false when the intent asks for nothing this activity handles. */
internal fun MainActivity.handleIntentAction(intent: Intent, navigator: Navigator): Boolean {
    val notificationId = intent.getIntExtra("notificationId", -1)
    if (notificationId > -1) {
        NotificationReceiver.dismissNotification(applicationContext, notificationId, intent.getIntExtra("groupId", 0))
    }
    when (val target = intentTarget(intent)) {
        is IntentTarget.Tab -> {
            if (target.popToRoot) navigator.popUntilRoot()
            lifecycleScope.launch { HomeScreen.openTab(target.tab) }
        }
        is IntentTarget.Push -> {
            navigator.popUntilRoot()
            navigator.push(target.screen)
        }
        IntentTarget.Handled -> {
            // Nothing to navigate to; the intent was consumed.
        }
        IntentTarget.Unhandled -> {
            return false
        }
    }
    ready = true
    return true
}

private fun intentTarget(intent: Intent): IntentTarget {
    val action = intent.action
    val shortcutTab = action?.let(shortcutTabs::get)
    return when {
        shortcutTab != null -> IntentTarget.Tab(shortcutTab())
        action == Constants.SHORTCUT_MANGA -> intent.extras?.getLong(Constants.MANGA_EXTRA)
            ?.let { IntentTarget.Tab(HomeScreen.Tab.Library(it), popToRoot = true) }
            ?: IntentTarget.Unhandled
        action == Constants.SHORTCUT_DOWNLOADS -> IntentTarget.Tab(
            HomeScreen.Tab.More(
                toDownloads =
                true,
            ),
            popToRoot = true,
        )
        action == Intent.ACTION_APPLICATION_PREFERENCES -> IntentTarget.Push(SettingsScreen())
        action == Intent.ACTION_SEARCH || action == Intent.ACTION_SEND || action == GOOGLE_SEARCH_ACTION ->
            searchTarget(intent)
        action == MainActivity.INTENT_SEARCH -> internalSearchTarget(intent)
        action == Intent.ACTION_VIEW -> viewTarget(intent)
        else -> IntentTarget.Unhandled
    }
}

// Get the search query provided in extras, and if not null, perform a global search with it.
private fun searchTarget(intent: Intent): IntentTarget {
    val query = intent.getStringExtra(SearchManager.QUERY) ?: intent.getStringExtra(Intent.EXTRA_TEXT)
    return if (query.isNullOrEmpty()) IntentTarget.Handled else IntentTarget.Push(DeepLinkScreen(query))
}

private fun internalSearchTarget(intent: Intent): IntentTarget {
    val query = intent.getStringExtra(MainActivity.INTENT_SEARCH_QUERY)
    if (query.isNullOrEmpty()) return IntentTarget.Handled
    return IntentTarget.Push(GlobalSearchScreen(query, intent.getStringExtra(MainActivity.INTENT_SEARCH_FILTER)))
}

// Opening a backup file, or the deep link that adds an extension store.
private fun viewTarget(intent: Intent): IntentTarget {
    val repoUrl = intent.takeIf { it.isAddExtensionStoreIntent() }?.data?.getQueryParameter("url")
    return when {
        intent.data.toString().endsWith(".tachibk") -> IntentTarget.Push(RestoreBackupScreen(intent.data.toString()))
        repoUrl != null -> IntentTarget.Push(ExtensionStoresScreen(repoUrl))
        else -> IntentTarget.Handled
    }
}

private fun Intent.isAddExtensionStoreIntent(): Boolean {
    return (scheme == "tachiyomi" && data?.host == "add-repo") ||
        (scheme == "mihon" && data?.host == "extension-store")
}
