package eu.kanade.tachiyomi.core.security

import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

public class SecurityPreferences(
    preferenceStore: PreferenceStore,
) {

    public val useAuthenticator: Preference<Boolean> = preferenceStore.getBoolean("use_biometric_lock", false)

    public val lockAppAfter: Preference<Int> = preferenceStore.getInt("lock_app_after", 0)

    public val secureScreen: Preference<SecureScreenMode> = preferenceStore.getEnum(
        "secure_screen_v2",
        SecureScreenMode.INCOGNITO,
    )

    public val hideNotificationContent: Preference<Boolean> = preferenceStore.getBoolean("hide_notification_content", false)

    // SY -->
    public val authenticatorTimeRanges: Preference<Set<String>> = preferenceStore.getStringSet("biometric_time_ranges", mutableSetOf())

    public val authenticatorDays: Preference<Int> = preferenceStore.getInt("biometric_days", 0x7F)

    public val encryptDatabase: Preference<Boolean> = preferenceStore.getBoolean(Preference.appStateKey("encrypt_database"), false)

    public val sqlPassword: Preference<String> = preferenceStore.getString(Preference.appStateKey("sql_password"), "")

    public val passwordProtectDownloads: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.privateKey("password_protect_downloads"),
        false,
    )

    public val encryptionType: Preference<EncryptionType> = preferenceStore.getEnum("encryption_type", EncryptionType.AES_256)

    public val cbzPassword: Preference<String> = preferenceStore.getString(Preference.appStateKey("cbz_password"), "")
    // SY <--

    /**
     * For app lock. Will be set when there is a pending timed lock.
     * Otherwise, this pref should be deleted.
     */
    public val lastAppClosed: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("last_app_closed"),
        0,
    )

    public enum class SecureScreenMode(public val titleRes: StringResource) {
        ALWAYS(MR.strings.lock_always),
        INCOGNITO(MR.strings.pref_incognito_mode),
        NEVER(MR.strings.lock_never),
    }

    // SY -->
    public enum class EncryptionType(public val titleRes: StringResource) {
        AES_256(SYMR.strings.aes_256),
        AES_128(SYMR.strings.aes_128),
        ZIP_STANDARD(SYMR.strings.standard_zip_encryption),
    }
    // SY <--
}
