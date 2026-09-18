package mihon.domain.extension.interactor

import mihon.domain.extension.repository.ExtensionStoreRepository

/** Removes an extension store by the url of its index. */
public class RemoveExtensionStore(
    private val repository: ExtensionStoreRepository,
) {
    /** Deletes the store at [indexUrl]. */
    public suspend operator fun invoke(indexUrl: String) {
        repository.remove(indexUrl)
    }
}
