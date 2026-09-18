package mihon.data.extension.model

import mihon.domain.extension.model.ExtensionStore

/** A store index in any of the network formats, convertible to the domain [ExtensionStore]. */
public interface BaseNetworkExtensionStore {
    /** The domain store for this index, identified by the [indexUrl] it was read from. */
    public fun toExtensionStore(indexUrl: String): ExtensionStore
}
