package eu.kanade.tachiyomi.ui.main

import android.content.Intent
import android.os.Looper
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.base.ActivityKoin
import eu.kanade.tachiyomi.ui.base.readObjectMember
import eu.kanade.tachiyomi.ui.home.HomeScreen
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * A [MainActivity] that is attached but never created, so [handleIntentAction] runs without the
 * activity's Compose tree; tab requests are read back from the home screen's own channel.
 */
internal class MainIntentRig {
    private val activityKoin = ActivityKoin()
    val navigator: Navigator = mockk(relaxed = true)
    private lateinit var controller: ActivityController<MainActivity>
    val activity: MainActivity get() = controller.get()

    fun start() {
        stopKoin()
        startKoin { modules(activityKoin.module()) }
        controller = Robolectric.buildActivity(MainActivity::class.java)
    }

    fun stop() {
        drainTab()
        stopKoin()
    }

    fun handle(intent: Intent): Boolean = activity.handleIntentAction(intent, navigator)

    /** The tab the last handled intent asked the home screen for, or null when it asked for none. */
    fun drainTab(): HomeScreen.Tab? {
        // The request is sent from the activity's lifecycle scope, dispatched on the main looper.
        shadowOf(Looper.getMainLooper()).idle()
        val channel = readObjectMember(HomeScreen::class, "openTabEvent") as Channel<*>
        return channel.tryReceive().getOrNull() as? HomeScreen.Tab
    }

    fun pushed(): Screen {
        val screen = slot<Screen>()
        verify { navigator.popUntilRoot() }
        verify { navigator.push(capture(screen)) }
        return screen.captured
    }
}
