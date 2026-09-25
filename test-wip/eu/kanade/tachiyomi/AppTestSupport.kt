package eu.kanade.tachiyomi

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider

/**
 * An [App] on the sandbox's base context, not yet created: Robolectric boots a bare
 * [Application] (robolectric.properties), so the real one is attached by hand.
 */
internal fun attachedApp(): App {
    val app = App()
    val base = ApplicationProvider.getApplicationContext<Application>().baseContext
    val attach = ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
    attach.isAccessible = true
    attach.invoke(app, base)
    return app
}
