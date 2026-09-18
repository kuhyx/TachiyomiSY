package mihon.domain.extension.interactor

import kotlinx.coroutines.flow.Flow
import mihon.domain.extension.repository.ExtensionStoreRepository

/** Counts the extension stores the user has added. */
public class GetExtensionStoreCountAsFlow(
    private val repository: ExtensionStoreRepository,
) {
    /** Number of stores, as a flow that re-emits on every change. */
    public operator fun invoke(): Flow<Long> = repository.getCountAsFlow()
}
