package eu.kanade.tachiyomi.ui.main

import android.app.NotificationManager
import android.content.Intent
import eu.kanade.tachiyomi.ui.home.HomeScreen
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.Constants

/** The intents that only switch the home screen's tab. */
@RunWith(RobolectricTestRunner::class)
internal class MainActivityIntentsTest {
    private val rig = MainIntentRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun tabFor(action: String?): HomeScreen.Tab? {
        rig.handle(Intent(action)) shouldBe true
        rig.activity.ready shouldBe true
        return rig.drainTab()
    }

    @Test
    fun shortcutsOpenTheirTabs() {
        tabFor(Constants.SHORTCUT_LIBRARY) shouldBe HomeScreen.Tab.Library()
        tabFor(Constants.SHORTCUT_UPDATES) shouldBe HomeScreen.Tab.Updates
        tabFor(Constants.SHORTCUT_HISTORY) shouldBe HomeScreen.Tab.History
        tabFor(Constants.SHORTCUT_SOURCES) shouldBe HomeScreen.Tab.Browse(false)
        tabFor(Constants.SHORTCUT_EXTENSIONS) shouldBe HomeScreen.Tab.Browse(true)
        verify(exactly = 0) { rig.navigator.popUntilRoot() }
    }

    @Test
    fun downloadsPopToRoot() {
        tabFor(Constants.SHORTCUT_DOWNLOADS) shouldBe HomeScreen.Tab.More(toDownloads = true)
        verify { rig.navigator.popUntilRoot() }
    }

    @Test
    fun mangaShortcutOpensManga() {
        val intent = Intent(Constants.SHORTCUT_MANGA).putExtra(Constants.MANGA_EXTRA, 7L)
        rig.handle(intent) shouldBe true
        rig.drainTab() shouldBe HomeScreen.Tab.Library(7L)
        verify { rig.navigator.popUntilRoot() }
    }

    @Test
    fun mangaShortcutNeedsExtras() {
        rig.handle(Intent(Constants.SHORTCUT_MANGA)) shouldBe false
        rig.activity.ready shouldBe false
        rig.drainTab().shouldBeNull()
    }

    @Test
    fun unknownActionsAreUnhandled() {
        rig.handle(Intent()) shouldBe false
        rig.handle(Intent("elsewhere")) shouldBe false
        rig.activity.ready shouldBe false
    }

    @Test
    fun notificationIsDismissed() {
        val manager = shadowOf(rig.activity.getSystemService(NotificationManager::class.java))
        val intent = Intent(Constants.SHORTCUT_UPDATES).putExtra("notificationId", 3).putExtra("groupId", 4)
        tabFor(intent.action) shouldBe HomeScreen.Tab.Updates
        rig.handle(intent) shouldBe true
        rig.drainTab() shouldBe HomeScreen.Tab.Updates
        manager.allNotifications.size shouldBe 0
    }
}
