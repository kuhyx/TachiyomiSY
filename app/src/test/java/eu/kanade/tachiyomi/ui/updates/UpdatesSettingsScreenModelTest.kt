package eu.kanade.tachiyomi.ui.updates

import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.updates.service.UpdatesPreferences

internal class UpdatesSettingsScreenModelTest {
    private val preferences = UpdatesPreferences(MapPreferenceStore())

    @AfterEach
    fun tearDown() = stopKoin()

    @Test
    fun toggleCyclesTheFilter() {
        val model = UpdatesSettingsScreenModel(preferences)
        model.toggleFilter(UpdatesPreferences::filterUnread)
        preferences.filterUnread.get() shouldBe TriState.ENABLED_IS
        model.toggleFilter(UpdatesPreferences::filterUnread)
        preferences.filterUnread.get() shouldBe TriState.ENABLED_NOT
    }

    @Test
    fun defaultsComeFromInjekt() {
        startKoin { modules(module { single { preferences } }) }
        UpdatesSettingsScreenModel().updatesPreferences shouldBe preferences
    }
}
