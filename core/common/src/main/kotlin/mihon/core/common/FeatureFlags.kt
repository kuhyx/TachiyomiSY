package mihon.core.common

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Build-time feature toggles and installation identity. */
public object FeatureFlags {

    /** A fresh random installation id. */
    @OptIn(ExperimentalUuidApi::class)
    public fun newInstallationId(): String = Uuid.random().toHexDashString()
}
