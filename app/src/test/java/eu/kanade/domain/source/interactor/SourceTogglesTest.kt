package eu.kanade.domain.source.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** The small preference-flipping interactors of the sources screen. */
internal class SourceTogglesTest {

    private val preferences = SourcePreferences(FlowPreferenceStore())

    @Test
    fun toggleSourceFlipsOrForces() {
        val toggle = ToggleSource(preferences)
        toggle.await(source(1))
        preferences.disabledSources.get() shouldBe setOf("1")
        toggle.await(source(1))
        preferences.disabledSources.get() shouldBe emptySet()
        toggle.await(2L)
        preferences.disabledSources.get() shouldBe setOf("2")
        toggle.await(2L)
        preferences.disabledSources.get() shouldBe emptySet()
        toggle.await(source(3), enable = false)
        toggle.await(4L, enable = false)
        preferences.disabledSources.get() shouldBe setOf("3", "4")
        toggle.await(listOf(3L, 5L), enable = false)
        preferences.disabledSources.get() shouldBe setOf("3", "4", "5")
        toggle.await(listOf(3L, 4L), enable = true)
        preferences.disabledSources.get() shouldBe setOf("5")
    }

    @Test
    fun toggleLanguage() {
        val toggle = ToggleLanguage(preferences)
        preferences.enabledLanguages.set(setOf("en"))
        toggle.await("fr")
        preferences.enabledLanguages.get() shouldBe setOf("en", "fr")
        toggle.await("en")
        preferences.enabledLanguages.get() shouldBe setOf("fr")
    }

    @Test
    fun toggleSourcePin() {
        val toggle = ToggleSourcePin(preferences)
        toggle.await(source(1))
        preferences.pinnedSources.get() shouldBe setOf("1")
        toggle.await(source(1))
        preferences.pinnedSources.get() shouldBe emptySet()
    }

    @Test
    fun toggleIncognito() {
        val toggle = ToggleIncognito(preferences)
        toggle.await("pkg", enable = true)
        preferences.incognitoExtensions.get() shouldBe setOf("pkg")
        toggle.await("pkg", enable = false)
        preferences.incognitoExtensions.get() shouldBe emptySet()
    }

    @Test
    fun toggleExcludeFromDataSaver() {
        val toggle = ToggleExcludeFromDataSaver(preferences)
        toggle.await(source(1))
        preferences.dataSaverExcludedSources.get() shouldBe setOf("1")
        toggle.await(source(1))
        preferences.dataSaverExcludedSources.get() shouldBe emptySet()
    }

    @Test
    fun setMigrateSorting() {
        SetMigrateSorting(preferences).await(SetMigrateSorting.Mode.TOTAL, SetMigrateSorting.Direction.DESCENDING)
        preferences.migrationSortingMode.get() shouldBe SetMigrateSorting.Mode.TOTAL
        preferences.migrationSortingDirection.get() shouldBe SetMigrateSorting.Direction.DESCENDING
        SetMigrateSorting.Mode.valueOf("ALPHABETICAL") shouldBe SetMigrateSorting.Mode.ALPHABETICAL
        SetMigrateSorting.Direction.valueOf("ASCENDING") shouldBe SetMigrateSorting.Direction.ASCENDING
    }

    @Test
    fun showLatestNeedsOldNavigation() = runTest {
        val uiPreferences = UiPreferences(FlowPreferenceStore())
        val getShowLatest = GetShowLatest(uiPreferences)
        getShowLatest.subscribe(hasSmartSearchConfig = false).first() shouldBe false
        uiPreferences.useNewSourceNavigation.set(false)
        getShowLatest.subscribe(hasSmartSearchConfig = false).first() shouldBe true
        getShowLatest.subscribe(hasSmartSearchConfig = true).first() shouldBe false
    }
}
