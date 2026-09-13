package eu.kanade.tachiyomi.core.security

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

public class PrivacyPreferences(
    preferenceStore: PreferenceStore,
) {
    public val crashlytics: Preference<Boolean> = preferenceStore.getBoolean("crashlytics", true)

    public val analytics: Preference<Boolean> = preferenceStore.getBoolean("analytics", true)
}
