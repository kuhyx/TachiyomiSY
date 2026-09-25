package eu.kanade.domain.track.service

import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.Preference

internal class TrackPreferencesTest {

    private val preferences = TrackPreferences(InMemoryPreferenceStore())
    private val tracker = mockk<Tracker> { every { id } returns 4L }

    @Test
    fun defaults() {
        preferences.anilistScoreType.get() shouldBe "POINT_10"
        preferences.mangabakaScoreType.get() shouldBe "STEP_1"
        preferences.autoUpdateTrack.get() shouldBe true
        preferences.autoUpdateTrackOnMarkRead.get() shouldBe AutoTrackState.ALWAYS
        preferences.resolveUsingSourceMetadata.get() shouldBe true
    }

    @Test
    fun perTrackerKeysArePrivate() {
        preferences.trackUsername(tracker).key() shouldBe Preference.privateKey("pref_mangasync_username_4")
        preferences.trackDisplayUsername(tracker).key() shouldBe Preference.privateKey("pref_mangasync_displayname_4")
        preferences.trackPassword(tracker).key() shouldBe Preference.privateKey("pref_mangasync_password_4")
        preferences.trackAuthExpired(tracker).key() shouldBe Preference.privateKey("pref_tracker_auth_expired_4")
        preferences.trackToken(tracker).key() shouldBe Preference.privateKey("track_token_4")
        preferences.trackUsername(tracker).get() shouldBe ""
        preferences.trackAuthExpired(tracker).get() shouldBe false
    }

    @Test
    fun setCredentialsWritesAllThree() {
        val store = spyk(InMemoryPreferenceStore())
        val username = InMemoryPreference("u", null, "")
        val password = InMemoryPreference("p", null, "")
        val expired = InMemoryPreference("e", true, false)
        every { store.getString(Preference.privateKey("pref_mangasync_username_4"), "") } returns username
        every { store.getString(Preference.privateKey("pref_mangasync_password_4"), "") } returns password
        every { store.getBoolean(Preference.privateKey("pref_tracker_auth_expired_4"), false) } returns expired
        TrackPreferences(store).setCredentials(tracker, "name", "secret")
        username.get() shouldBe "name"
        password.get() shouldBe "secret"
        expired.get() shouldBe false
    }
}
