package tachiyomi.domain.category.model

import java.io.Serializable

/**
 * A user-defined library category; [UNCATEGORIZED_ID] is the built-in one.
 *
 * @property id Row id; 0 until inserted.
 * @property name Display name; empty for the built-in category.
 * @property order Position in the category list, 0-based.
 * @property flags Per-category library sort bits ([tachiyomi.domain.library.model.LibrarySort]).
 * @property version Sync version counter.
 * @property uid Random id that stays stable across devices, for sync.
 * @property lastModifiedAt Epoch seconds of the last row change, for sync.
 */
public data class Category(
    val id: Long,
    val name: String,
    val order: Long,
    val flags: Long,
    val version: Long = 0,
    val uid: Long = 0,
    val lastModifiedAt: Long = 0,
) : Serializable {

    /** Whether this is the built-in category, which cannot be renamed, moved or deleted. */
    val isSystemCategory: Boolean = id == UNCATEGORIZED_ID

    /** The built-in category's id. */
    public companion object {
        private const val serialVersionUID: Long = 1L

        /** Id of the built-in "default" category every uncategorised favourite belongs to. */
        public const val UNCATEGORIZED_ID: Long = 0L
    }
}
