package eu.kanade.tachiyomi.ui.main

import android.app.SearchManager
import android.content.Intent
import androidx.core.net.toUri
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.tachiyomi.source.online.readMember
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.deeplink.DeepLinkScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The intents that push a screen over the home screen, and the ones that are consumed without one. */
@RunWith(RobolectricTestRunner::class)
internal class MainActivityPushTest {
    private val rig = MainIntentRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun consumed(intent: Intent) {
        clearMocks(rig.navigator)
        rig.handle(intent) shouldBe true
        verify(exactly = 0) { rig.navigator.push(any<Screen>()) }
    }

    private fun view(uri: String) = Intent(Intent.ACTION_VIEW, uri.toUri())

    @Test
    fun preferencesOpenSettings() {
        rig.handle(Intent(Intent.ACTION_APPLICATION_PREFERENCES)) shouldBe true
        rig.pushed().shouldBeInstanceOf<SettingsScreen>()
    }

    @Test
    fun searchQueryOpensDeepLink() {
        rig.handle(Intent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, "one")) shouldBe true
        rig.pushed().shouldBeInstanceOf<DeepLinkScreen>().query shouldBe "one"
    }

    @Test
    fun sharedTextOpensDeepLink() {
        rig.handle(Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, "two")) shouldBe true
        rig.pushed().shouldBeInstanceOf<DeepLinkScreen>().query shouldBe "two"
    }

    @Test
    fun blankSearchesAreConsumed() {
        consumed(Intent("com.google.android.gms.actions.SEARCH_ACTION"))
        consumed(Intent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, ""))
        consumed(Intent(MainActivity.INTENT_SEARCH))
        consumed(Intent(MainActivity.INTENT_SEARCH).putExtra(MainActivity.INTENT_SEARCH_QUERY, ""))
    }

    @Test
    fun internalSearchOpensGlobal() {
        val intent = Intent(MainActivity.INTENT_SEARCH)
            .putExtra(MainActivity.INTENT_SEARCH_QUERY, "three")
            .putExtra(MainActivity.INTENT_SEARCH_FILTER, "ext")
        rig.handle(intent) shouldBe true
        val screen = rig.pushed().shouldBeInstanceOf<GlobalSearchScreen>()
        screen.searchQuery shouldBe "three"
        screen.readMember(GlobalSearchScreen::class, "extensionFilter") shouldBe "ext"
    }

    @Test
    fun backupFileOpensRestore() {
        rig.handle(view("content://files/a.tachibk")) shouldBe true
        val screen = rig.pushed().shouldBeInstanceOf<RestoreBackupScreen>()
        screen.readMember(RestoreBackupScreen::class, "uri") shouldBe "content://files/a.tachibk"
    }

    @Test
    fun tachiyomiRepoLinkAddsStore() {
        rig.handle(view("tachiyomi://add-repo?url=https%3A%2F%2Frepo")) shouldBe true
        val screen = rig.pushed().shouldBeInstanceOf<ExtensionStoresScreen>()
        screen.readMember(ExtensionStoresScreen::class, "url") shouldBe "https://repo"
    }

    @Test
    fun mihonStoreLinkAddsStore() {
        rig.handle(view("mihon://extension-store?url=https%3A%2F%2Fstore")) shouldBe true
        val screen = rig.pushed().shouldBeInstanceOf<ExtensionStoresScreen>()
        screen.readMember(ExtensionStoresScreen::class, "url") shouldBe "https://store"
    }

    @Test
    fun otherLinksAreConsumed() {
        consumed(Intent(Intent.ACTION_VIEW))
        consumed(view("tachiyomi://elsewhere?url=x"))
        consumed(view("mihon://elsewhere?url=x"))
        consumed(view("https://add-repo?url=x"))
        consumed(view("tachiyomi://add-repo"))
    }
}
