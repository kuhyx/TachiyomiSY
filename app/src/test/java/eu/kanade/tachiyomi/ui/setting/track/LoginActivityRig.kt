package eu.kanade.tachiyomi.ui.setting.track

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.base.ActivityKoin
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowChoreographer

/**
 * Launches an OAuth callback activity for a URI over the base activity's Koin and a relaxed tracker
 * manager; the activity's spinner never stops, so the choreographer is paused while one is up.
 */
internal class LoginActivityRig(private val extra: Module = module {}) {
    val trackers: TrackerManager = mockk(relaxed = true)
    private val activityKoin = ActivityKoin()
    private val launched = mutableListOf<ActivityController<out Activity>>()

    fun start() {
        ShadowChoreographer.setPaused(true)
        stopKoin()
        startKoin { modules(activityKoin.module(), module { single { trackers } }, extra) }
    }

    fun stop() {
        try {
            // Their spinners would animate forever once the choreographer runs again.
            launched.forEach { it.pause().stop().destroy() }
            launched.clear()
            stopKoin()
        } finally {
            ShadowChoreographer.setPaused(false)
        }
    }

    fun <A : Activity> launch(type: Class<A>, uri: String?): A {
        val intent = Intent(Intent.ACTION_VIEW, uri?.toUri())
        val controller = Robolectric.buildActivity(type, intent).setup()
        launched += controller
        return controller.get()
    }

    /** Waits until [activity] has finished and asked for the main activity back on top. */
    fun returned(activity: Activity) {
        eventually { activity.isFinishing }
        val next = shadowOf(activity).nextStartedActivity
        next.component?.className shouldBe MainActivity::class.java.name
        (next.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) shouldBe Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}
