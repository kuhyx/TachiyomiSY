package eu.kanade.presentation.more.onboarding

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.storage.service.StoragePreferences

/** The preferences the onboarding steps read, over one shared store. */
internal class OnboardingKoin {
    private val store = MapPreferenceStore()
    val storage: StoragePreferences = StoragePreferences(mockk(relaxed = true), store)
    val privacy: PrivacyPreferences = PrivacyPreferences(store)
    val ui: UiPreferences = UiPreferences(store)

    fun start() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { storage }
                    single { privacy }
                    single { ui }
                },
            )
        }
    }
}
