package eu.kanade.tachiyomi.ui.setting.track

import io.mockk.coVerify
import io.mockk.every
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Each tracker's OAuth callback logs in with what the URI carries, or logs out without it. */
@RunWith(RobolectricTestRunner::class)
internal class TrackLoginActivityTest {
    private val rig = LoginActivityRig()
    private val trackers get() = rig.trackers

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun open(uri: String?) = rig.returned(rig.launch(TrackLoginActivity::class.java, uri))

    @Test
    fun missingDataReturns() {
        open(null)
    }

    @Test
    fun aniListUsesFragmentToken() {
        open("tachiyomi://anilist-auth#access_token=tok&&expires")
        coVerify { trackers.aniList.login("tok") }
        open("tachiyomi://anilist-auth")
        coVerify { trackers.aniList.logout() }
    }

    @Test
    fun bangumiUsesQueryCode() {
        open("tachiyomi://bangumi-auth?code=c%20d")
        coVerify { trackers.bangumi.login("c d") }
        open("tachiyomi://bangumi-auth?other=1")
        coVerify { trackers.bangumi.logout() }
    }

    @Test
    fun myAnimeListAndShikimori() {
        open("tachiyomi://myanimelist-auth?code=m")
        open("tachiyomi://myanimelist-auth?x")
        open("tachiyomi://shikimori-auth?code=s")
        open("tachiyomi://shikimori-auth?x")
        coVerify {
            trackers.myAnimeList.login("m")
            trackers.myAnimeList.logout()
            trackers.shikimori.login("s")
            trackers.shikimori.logout()
        }
    }

    @Test
    fun hikkaUsesReference() {
        open("tachiyomi://hikka-auth?reference=r")
        open("tachiyomi://hikka-auth?x")
        coVerify {
            trackers.hikka.login("r")
            trackers.hikka.logout()
        }
    }

    @Test
    fun mangaBakaChecksState() {
        val baka = trackers.mangaBaka
        every { baka.verifyOAuthState("good") } returns true
        every { baka.verifyOAuthState("bad") } returns false
        open("tachiyomi://mangabaka-auth?code=k")
        open("tachiyomi://mangabaka-auth?code=k&state=bad")
        coVerify(exactly = 0) { baka.login(any<String>()) }
        open("tachiyomi://mangabaka-auth?code=k&state=good")
        coVerify { baka.login("k") }
        open("tachiyomi://mangabaka-auth?state=good")
        coVerify { baka.logout() }
    }

    @Test
    fun unknownHostOnlyReturns() {
        open("tachiyomi://elsewhere?code=1")
    }
}
