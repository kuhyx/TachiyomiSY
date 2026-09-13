package eu.kanade.tachiyomi.network

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** Networking settings: logging, DNS-over-HTTPS and the User-Agent. */
public class NetworkPreferences(
    preferenceStore: PreferenceStore,
    verboseLoggingDefault: Boolean = false,
) {

    /** Log request and response bodies. */
    public val verboseLogging: Preference<Boolean> =
        preferenceStore.getBoolean("verbose_logging", verboseLoggingDefault)

    /** Selected DNS-over-HTTPS provider (`PREF_DOH_*`), -1 for none. */
    public val dohProvider: Preference<Int> = preferenceStore.getInt("doh_provider", -1)

    /** The User-Agent sent when a request sets none. */
    public val defaultUserAgent: Preference<String> = preferenceStore.getString(
        "default_user_agent",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/141.0.0.0 Mobile Safari/537.36",
    )
}
