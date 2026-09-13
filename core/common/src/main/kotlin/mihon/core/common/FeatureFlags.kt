package mihon.core.common

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

public object FeatureFlags {

    @OptIn(ExperimentalUuidApi::class)
    public fun newInstallationId(): String {
        return Uuid.random().toHexDashString()
    }
}
