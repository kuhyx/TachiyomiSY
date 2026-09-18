package eu.kanade.tachiyomi.util.storage

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope

/**
 * The one [SecurityPreferences] every crypto test injects. [CbzCrypto] is an object that
 * resolves its preferences lazily once per class loader, so all tests must agree on the instance.
 */
internal object CryptoTestEnv {
    val prefs: SecurityPreferences = SecurityPreferences(FlowPreferenceStore())
    private var previous: InjektScope? = null

    /** Registers the fake keystore, injects [prefs] and clears every preference. */
    fun install() {
        installFakeAndroidKeyStore()
        val registrar = mockk<InjektRegistrar>()
        every { registrar.getInstance<SecurityPreferences>(any()) } returns prefs
        previous = Injekt
        Injekt = InjektScope(registrar)
        reset()
    }

    /** Puts the Injekt scope that was active before [install] back. */
    fun restore() {
        val scope = previous ?: return
        Injekt = scope
        previous = null
    }

    /** Deletes the preferences the crypto code reads. */
    fun reset() {
        prefs.cbzPassword.delete()
        prefs.sqlPassword.delete()
        prefs.passwordProtectDownloads.delete()
        prefs.encryptionType.delete()
    }
}

/** A scope whose coroutines run inline, so eagerly started flows settle synchronously. */
internal fun inlineScope(dispatcher: CoroutineDispatcher = Dispatchers.Unconfined): CoroutineScope =
    CoroutineScope(dispatcher)
