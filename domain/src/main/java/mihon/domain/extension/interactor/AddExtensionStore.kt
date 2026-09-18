package mihon.domain.extension.interactor

import mihon.domain.extension.repository.ExtensionStoreRepository

/** Adds an extension store by the url of its index. */
public class AddExtensionStore(
    private val repository: ExtensionStoreRepository,
) {
    /** Fetches and stores the store at [indexUrl]; a failed fetch is the returned [Result]'s failure. */
    public suspend operator fun invoke(indexUrl: String): Result<Unit> = repository.insert(indexUrl)
}
