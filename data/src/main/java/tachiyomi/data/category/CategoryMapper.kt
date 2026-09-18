package tachiyomi.data.category

import tachiyomi.data.GetCategories
import tachiyomi.data.GetCategoriesByMangaId
import tachiyomi.data.GetCategory
import tachiyomi.domain.category.model.Category

/** Domain models from the generated `categories` query rows; one overload per query row shape. */
public object CategoryMapper {
    /** The [Category] of a `getCategory` row. */
    public fun mapCategory(row: GetCategory): Category = Category(
        id = row._id,
        name = row.name,
        order = row.sort,
        flags = row.flags,
        version = row.version,
        uid = row.uid,
        lastModifiedAt = row.last_modified_at,
    )

    /** The [Category] of a `getCategories` row. */
    public fun mapCategory(row: GetCategories): Category = Category(
        id = row.id,
        name = row.name,
        order = row.order,
        flags = row.flags,
        version = row.version,
        uid = row.uid,
        lastModifiedAt = row.last_modified_at,
    )

    /** The [Category] of a `getCategoriesByMangaId` row. */
    public fun mapCategory(row: GetCategoriesByMangaId): Category = Category(
        id = row.id,
        name = row.name,
        order = row.order,
        flags = row.flags,
        version = row.version,
        uid = row.uid,
        lastModifiedAt = row.last_modified_at,
    )
}
