package exh.ui

import android.app.Application
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore

/** What every [eu.kanade.tachiyomi.ui.base.activity.BaseActivity] pulls from Injekt, on in-memory preferences. */
internal fun baseActivityModule(context: Application): Module {
    val store = InMemoryPreferenceStore()
    return module {
        single { UiPreferences(store) }
        single { SecurityPreferences(store) }
        single { BasePreferences(context, store) }
    }
}
