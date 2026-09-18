package tachiyomi.domain.category.interactor

import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences

/** Changes the library display mode. */
public class SetDisplayMode(
    private val preferences: LibraryPreferences,
) {

    /** Stores [display] as the `displayMode` preference. */
    public fun await(display: LibraryDisplayMode) {
        preferences.displayMode.set(display)
    }
}
