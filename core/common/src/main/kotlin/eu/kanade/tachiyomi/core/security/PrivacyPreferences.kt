package eu.kanade.tachiyomi.core.security

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** Opt-in switches for crash and usage reporting. */
public class PrivacyPreferences(
    preferenceStore: PreferenceStore,
) {
    /** Whether crash reports may be sent. */
    public val crashlytics: Preference<Boolean> = preferenceStore.getBoolean("crashlytics", true)

    /** Whether anonymous usage analytics may be sent. */
    public val analytics: Preference<Boolean> = preferenceStore.getBoolean("analytics", true)
}
