package tachiyomi.domain.category.interactor

import tachiyomi.domain.category.model.Category

/** A user category with the given id; the order defaults to the id. */
internal fun testCategory(id: Long, order: Long = id, flags: Long = 0L): Category = Category(
    id = id,
    name = "category $id",
    order = order,
    flags = flags,
)
