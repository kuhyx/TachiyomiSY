package eu.kanade.presentation.more.settings

import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class PreferenceModelTest {
    @Test
    fun noOpCallbacksAcceptAnything() = runTest {
        Preference.PreferenceItem.TextPreference(title = "t").onValueChanged("x") shouldBe Unit
        Preference.PreferenceItem.InfoPreference(title = "i").onValueChanged("x") shouldBe Unit
        Preference.PreferenceItem.CustomPreference(title = "c") {}.onValueChanged(Unit) shouldBe Unit
        Preference.PreferenceItem.SliderPreference(value = 0, title = "s").onValueChanged(1) shouldBe Unit
        Preference.PreferenceItem.SliderPreference(value = 0, title = "s").icon shouldBe null
        Preference.PreferenceItem.BasicListPreference("a", emptyMap(), "b").onValueChanged("a") shouldBe Unit
        val tracker = Preference.PreferenceItem.TrackerPreference(mockk<Tracker>(), login = {}, logout = {})
        tracker.onValueChanged("x") shouldBe Unit
    }

    @Test
    fun fixedMembersOfLeafItems() {
        val tracker = Preference.PreferenceItem.TrackerPreference(mockk<Tracker>(), login = {}, logout = {})
        tracker.title shouldBe ""
        tracker.enabled shouldBe true
        tracker.subtitle shouldBe null
        tracker.icon shouldBe null
        val info = Preference.PreferenceItem.InfoPreference(title = "i")
        info.enabled shouldBe true
        info.subtitle shouldBe null
        info.icon shouldBe null
        val custom = Preference.PreferenceItem.CustomPreference(title = "c") {}
        custom.enabled shouldBe true
        custom.subtitle shouldBe null
        custom.icon shouldBe null
    }

    @Test
    fun sliderStepsFollowRange() {
        Preference.PreferenceItem.SliderPreference(value = 0, title = "s", valueRange = 2..7).steps shouldBe 4
    }

    @Test
    fun groupDefaultsToEnabled() {
        Preference.PreferenceGroup(title = "g", preferenceItems = emptyList()).enabled shouldBe true
    }
}
