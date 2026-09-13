package eu.kanade.tachiyomi.core.security

import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

// Seven set bits, one per weekday.
private const val ALL_WEEKDAYS = 0x7F

/** App lock, screen secrecy and the SY archive/database encryption settings. */
public class SecurityPreferences(
    preferenceStore: PreferenceStore,
) {

    /** Require biometric or credential unlock. */
    public val useAuthenticator: Preference<Boolean> = preferenceStore.getBoolean("use_biometric_lock", false)

    /** Minutes of background time before the lock re-arms; 0 locks immediately, -1 never. */
    public val lockAppAfter: Preference<Int> = preferenceStore.getInt("lock_app_after", 0)

    /** When the window is flagged secure (no screenshots, hidden in recents). */
    public val secureScreen: Preference<SecureScreenMode> = preferenceStore.getEnum(
        "secure_screen_v2",
        SecureScreenMode.INCOGNITO,
    )

    /** Hide chapter details from notifications. */
    public val hideNotificationContent: Preference<Boolean> =
        preferenceStore.getBoolean("hide_notification_content", false)

    // SY -->

    /** SY: time ranges during which the lock is active. */
    public val authenticatorTimeRanges: Preference<Set<String>> =
        preferenceStore.getStringSet("biometric_time_ranges", mutableSetOf())

    /** SY: bit mask of weekdays on which the lock is active. */
    public val authenticatorDays: Preference<Int> = preferenceStore.getInt("biometric_days", ALL_WEEKDAYS)

    /** SY: encrypt the app database with SQLCipher. */
    public val encryptDatabase: Preference<Boolean> =
        preferenceStore.getBoolean(Preference.appStateKey("encrypt_database"), false)

    /** SY: the stored, encrypted database password. */
    public val sqlPassword: Preference<String> = preferenceStore.getString(Preference.appStateKey("sql_password"), "")

    /** SY: password-protect downloaded CBZ archives. */
    public val passwordProtectDownloads: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.privateKey("password_protect_downloads"),
        false,
    )

    /** SY: cipher used for protected archives. */
    public val encryptionType: Preference<EncryptionType> =
        preferenceStore.getEnum("encryption_type", EncryptionType.AES_256)

    /** SY: the stored, encrypted archive password. */
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

    /**
     * When the app window is treated as secure.
     *
     * @property titleRes label of the option.
     */
    public enum class SecureScreenMode(public val titleRes: StringResource) {
        /** Always secure. */
        ALWAYS(MR.strings.lock_always),

        /** Secure only in incognito mode. */
        INCOGNITO(MR.strings.pref_incognito_mode),

        /** Never secure. */
        NEVER(MR.strings.lock_never),
    }

    // SY -->

    /**
     * Cipher used for password-protected archives.
     *
     * @property titleRes label of the option.
     */
    public enum class EncryptionType(public val titleRes: StringResource) {
        /** AES with a 256-bit key. */
        AES_256(SYMR.strings.aes_256),

        /** AES with a 128-bit key. */
        AES_128(SYMR.strings.aes_128),

        /** Legacy ZipCrypto. */
        ZIP_STANDARD(SYMR.strings.standard_zip_encryption),
    }
    // SY <--
}
