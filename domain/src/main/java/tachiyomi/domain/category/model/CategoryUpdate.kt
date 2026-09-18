package tachiyomi.domain.category.model

/**
 * A partial update of one [Category]: every null field is left as it is.
 *
 * @property id Id of the category to update.
 * @property name New display name.
 * @property order New position in the category list.
 * @property flags New library sort bits.
 * @property version New sync version counter.
 * @property uid New sync id.
 * @property lastModifiedAt New last-change epoch seconds.
 */
public data class CategoryUpdate(
    val id: Long,
    val name: String? = null,
    val order: Long? = null,
    val flags: Long? = null,
    val version: Long? = null,
    val uid: Long? = null,
    val lastModifiedAt: Long? = null,
)
