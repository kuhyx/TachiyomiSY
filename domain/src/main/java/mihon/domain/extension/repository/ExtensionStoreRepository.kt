package mihon.domain.extension.repository

import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.Flow
import mihon.domain.extension.model.ExtensionStore

/** Storage and network access for the user's extension stores, keyed by index url. */
public interface ExtensionStoreRepository {
    /** Fetches the store at [indexUrl] and upserts it; a failed fetch is the returned [Result]'s failure. */
    public suspend fun insert(indexUrl: String): Result<Unit>

    /** Upserts a placeholder store for [indexUrl] called [name], for migrating the old url preference. */
    public suspend fun insertFromPreference(indexUrl: String, name: String)

    /** Re-fetches every store's index; a store that fails is logged and left as it was. */
    public suspend fun refreshAll()

    /** Every extension every store offers; a store that fails is logged and skipped. */
    public suspend fun fetchExtensions(): List<Extension.Available>

    /** Every store the user has added. */
    public suspend fun getAll(): List<ExtensionStore>

    /** [getAll] as a flow that re-emits on every change. */
    public fun getAllAsFlow(): Flow<List<ExtensionStore>>

    /** Number of stores, as a flow that re-emits on every change. */
    public fun getCountAsFlow(): Flow<Long>

    /** Deletes the store at [indexUrl]. */
    public suspend fun remove(indexUrl: String)
}
