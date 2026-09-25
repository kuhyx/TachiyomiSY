package eu.kanade.tachiyomi.ui.base

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * What every `BaseActivity` pulls out of Injekt before its own dependencies: the theme, the
 * tablet-UI switch, the secure-screen and app-lock settings, all real and in memory.
 */
internal class ActivityKoin(
    val application: Application = ApplicationProvider.getApplicationContext(),
) {
    val store: MapPreferenceStore = MapPreferenceStore()
    val ui: UiPreferences = UiPreferences(store)
    val security: SecurityPreferences = SecurityPreferences(store)
    val base: BasePreferences = BasePreferences(application, store)

    fun module(): Module = module {
        single { application }
        single { ui }
        single { security }
        single { base }
    }
}
