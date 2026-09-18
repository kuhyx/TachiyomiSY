package exh.pref

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class DelegateSourcePreferencesTest {
    private val store = InMemoryPreferenceStore(
        sequenceOf(InMemoryPreferenceStore.InMemoryPreference("use_jp_title", true, false)),
    )
    private val preferences = DelegateSourcePreferences(store)

    @Test
    fun readsBothPreferences() {
        preferences.delegateSources.key() shouldBe "eh_delegate_sources"
        preferences.delegateSources.get() shouldBe true
        preferences.delegateSources.isSet() shouldBe false
        preferences.useJapaneseTitle.key() shouldBe "use_jp_title"
        preferences.useJapaneseTitle.get() shouldBe true
        preferences.useJapaneseTitle.isSet() shouldBe true
    }

    @Test
    fun dataClassMembers() {
        val same = DelegateSourcePreferences(store)
        preferences shouldBe same
        preferences.hashCode() shouldBe same.hashCode()
        preferences.toString() shouldStartWith "DelegateSourcePreferences(preferenceStore="
        preferences.copy() shouldBe preferences
        preferences.copy(preferenceStore = InMemoryPreferenceStore()) shouldNotBe preferences
        val component = DelegateSourcePreferences::class.java.getDeclaredMethod("component1")
        component.isAccessible = true
        component.invoke(preferences) shouldBe store
    }
}
