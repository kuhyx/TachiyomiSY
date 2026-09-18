package exh.metadata.metadata

import android.content.Context
import android.content.res.Resources
import exh.pref.DelegateSourcePreferences
import io.mockk.every
import io.mockk.mockk
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope

/** Prefix every stubbed string resource resolves to; the suffix is the resource id. */
internal const val LABEL_PREFIX: String = "label#"

/**
 * Replaces the global [Injekt] scope with one that serves a [DelegateSourcePreferences] whose
 * `useJapaneseTitle` answers [preferJapanese]. Returns the previous scope so a test can restore it.
 */
internal fun injectDelegatePrefs(preferJapanese: Boolean, delegateSources: Boolean = true): InjektScope {
    val prefs = DelegateSourcePreferences(
        mockk<PreferenceStore> {
            every { getBoolean("use_jp_title", any()) } returns booleanPreference(preferJapanese)
            every { getBoolean("eh_delegate_sources", any()) } returns booleanPreference(delegateSources)
        },
    )
    val registrar = mockk<InjektRegistrar> {
        every { getInstance<DelegateSourcePreferences>(any()) } returns prefs
    }
    val previous = Injekt
    Injekt = InjektScope(registrar)
    return previous
}

/** A [Context] whose string resources resolve to [LABEL_PREFIX] followed by the resource id. */
internal fun stubbedContext(): Context {
    val stubResources = mockk<Resources> {
        every { getString(any()) } answers { LABEL_PREFIX + firstArg<Int>() }
    }
    return mockk<Context> {
        every { resources } returns stubResources
    }
}

private fun booleanPreference(value: Boolean): Preference<Boolean> = mockk {
    every { get() } returns value
}
