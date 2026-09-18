package tachiyomi.domain.library.model

/** Which entries "update library" touches while the library is grouped by something other than categories. */
public enum class GroupLibraryMode {
    /** Ignore the current group; use the update-category preferences as when grouped by category. */
    GLOBAL,

    /** Only the current group, except the ungrouped view, which behaves like [GLOBAL]. */
    ALL_BUT_UNGROUPED,

    /** Only the current group, the ungrouped view included. */
    ALL,
}
