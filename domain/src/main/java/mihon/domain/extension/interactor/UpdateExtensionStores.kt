package mihon.domain.extension.interactor

import mihon.domain.extension.repository.ExtensionStoreRepository

/** Refreshes every extension store from its index. */
public class UpdateExtensionStores(
    private val repository: ExtensionStoreRepository,
) {
    /** Re-fetches every store; a store that fails is logged and left as it was. */
    public suspend operator fun invoke() {
        repository.refreshAll()
    }
}
