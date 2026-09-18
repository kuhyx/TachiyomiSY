package mihon.domain.extension.interactor

import kotlinx.coroutines.flow.Flow
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.repository.ExtensionStoreRepository

/** Lists the extension stores the user has added. */
public class GetExtensionStores(
    private val repository: ExtensionStoreRepository,
) {
    /** Every store. */
    public suspend fun get(): List<ExtensionStore> = repository.getAll()

    /** [get] as a flow that re-emits on every change. */
    public fun subscribe(): Flow<List<ExtensionStore>> = repository.getAllAsFlow()
}
