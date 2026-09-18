package tachiyomi.domain.library.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

/** The ways the library can be grouped (the `groupLibraryBy` preference) and their labels. */
public object LibraryGroup {

    /** By category. */
    public const val BY_DEFAULT: Int = 0

    /** By source. */
    public const val BY_SOURCE: Int = 1

    /** By publishing status. */
    public const val BY_STATUS: Int = 2

    /** By tracker status. */
    public const val BY_TRACK_STATUS: Int = 3

    /** One flat list. */
    public const val UNGROUPED: Int = 4

    /**
     * The tab label for grouping [type]; [BY_DEFAULT] reads "ungrouped" when the user has no
     * categories ([hasCategories] false).
     */
    public fun groupTypeStringRes(type: Int, hasCategories: Boolean = true): StringResource {
        return when (type) {
            BY_STATUS -> MR.strings.status
            BY_SOURCE -> MR.strings.label_sources
            BY_TRACK_STATUS -> SYMR.strings.tracking_status
            UNGROUPED -> SYMR.strings.ungrouped
            else -> if (hasCategories) MR.strings.categories else SYMR.strings.ungrouped
        }
    }
}
