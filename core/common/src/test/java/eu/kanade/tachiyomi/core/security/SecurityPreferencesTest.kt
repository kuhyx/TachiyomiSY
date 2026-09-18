package eu.kanade.tachiyomi.core.security

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

internal class SecurityPreferencesTest {
    private val prefs = SecurityPreferences(InMemoryPreferenceStore())

    @Test
    fun lockDefaults() {
        prefs.useAuthenticator.get() shouldBe false
        prefs.lockAppAfter.get() shouldBe 0
        prefs.secureScreen.get() shouldBe SecurityPreferences.SecureScreenMode.INCOGNITO
        prefs.hideNotificationContent.get() shouldBe false
        prefs.lastAppClosed.get() shouldBe 0L
    }

    @Test
    fun lockKeys() {
        prefs.useAuthenticator.key() shouldBe "use_biometric_lock"
        prefs.lockAppAfter.key() shouldBe "lock_app_after"
        prefs.secureScreen.key() shouldBe "secure_screen_v2"
        prefs.hideNotificationContent.key() shouldBe "hide_notification_content"
        prefs.lastAppClosed.key() shouldBe "__APP_STATE_last_app_closed"
    }

    @Test
    fun syDefaults() {
        prefs.authenticatorTimeRanges.get() shouldBe emptySet()
        prefs.authenticatorDays.get() shouldBe 0x7F
        prefs.encryptDatabase.get() shouldBe false
        prefs.sqlPassword.get() shouldBe ""
        prefs.passwordProtectDownloads.get() shouldBe false
        prefs.encryptionType.get() shouldBe SecurityPreferences.EncryptionType.AES_256
        prefs.cbzPassword.get() shouldBe ""
    }

    @Test
    fun syKeys() {
        prefs.authenticatorTimeRanges.key() shouldBe "biometric_time_ranges"
        prefs.authenticatorDays.key() shouldBe "biometric_days"
        prefs.encryptDatabase.key() shouldBe "__APP_STATE_encrypt_database"
        prefs.sqlPassword.key() shouldBe "__APP_STATE_sql_password"
        prefs.passwordProtectDownloads.key() shouldBe "__PRIVATE_password_protect_downloads"
        prefs.encryptionType.key() shouldBe "encryption_type"
        prefs.cbzPassword.key() shouldBe "__APP_STATE_cbz_password"
    }

    @Test
    fun storesValues() {
        prefs.useAuthenticator.set(true)
        prefs.authenticatorTimeRanges.set(setOf("08:00-17:00"))
        prefs.encryptionType.set(SecurityPreferences.EncryptionType.ZIP_STANDARD)
        prefs.cbzPassword.set("cipher")
        prefs.useAuthenticator.get() shouldBe true
        prefs.authenticatorTimeRanges.get() shouldBe setOf("08:00-17:00")
        prefs.encryptionType.get() shouldBe SecurityPreferences.EncryptionType.ZIP_STANDARD
        prefs.cbzPassword.get() shouldBe "cipher"
    }

    @Test
    fun secureScreenTitles() {
        SecurityPreferences.SecureScreenMode.ALWAYS.titleRes shouldBe MR.strings.lock_always
        SecurityPreferences.SecureScreenMode.INCOGNITO.titleRes shouldBe MR.strings.pref_incognito_mode
        SecurityPreferences.SecureScreenMode.NEVER.titleRes shouldBe MR.strings.lock_never
        SecurityPreferences.SecureScreenMode.entries.size shouldBe 3
    }

    @Test
    fun encryptionTypeTitles() {
        SecurityPreferences.EncryptionType.AES_256.titleRes shouldBe SYMR.strings.aes_256
        SecurityPreferences.EncryptionType.AES_128.titleRes shouldBe SYMR.strings.aes_128
        SecurityPreferences.EncryptionType.ZIP_STANDARD.titleRes shouldBe SYMR.strings.standard_zip_encryption
        SecurityPreferences.EncryptionType.valueOf("AES_128") shouldBe SecurityPreferences.EncryptionType.AES_128
    }
}
