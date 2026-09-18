package tachiyomi.domain.category.interactor

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences

internal class SetDisplayModeTest {

    private val preferences = LibraryPreferences(InMemoryPreferenceStore())
    private val interactor = SetDisplayMode(preferences)

    @Test
    fun storesTheDisplayMode() {
        preferences.displayMode.get() shouldBe LibraryDisplayMode.CompactGrid

        interactor.await(LibraryDisplayMode.CoverOnlyGrid)

        preferences.displayMode.get() shouldBe LibraryDisplayMode.CoverOnlyGrid
    }

    @Test
    fun overwritesAPreviousMode() {
        interactor.await(LibraryDisplayMode.List)
        interactor.await(LibraryDisplayMode.ComfortableGrid)

        preferences.displayMode.get() shouldBe LibraryDisplayMode.ComfortableGrid
    }
}
