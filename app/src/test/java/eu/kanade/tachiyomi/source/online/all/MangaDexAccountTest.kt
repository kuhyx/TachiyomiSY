package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.sManga
import exh.md.dto.SAMPLE_DATA_JSON
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MangaDexAccountTest {
    private val harness = SourceTestHarness()
    private lateinit var fixture: MangaDexFixture
    private val source get() = fixture.source

    @Before
    fun setUp() {
        harness.install()
        fixture = MangaDexFixture(harness)
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun loginStateFromTracker() {
        every { fixture.mdList.isLoggedIn } returns true
        every { fixture.mdList.getUsername() } returns "user"
        every { fixture.mdList.getPassword() } returns "pass"
        source.isLogged() shouldBe true
        source.getUsername() shouldBe "user"
        source.getPassword() shouldBe "pass"
    }

    @Test
    fun loginAndLogout() {
        justRun { fixture.mdList.logout() }
        MdUtil.codeVerifier = "v"
        harness.enqueue("""{"token_type":"Bearer","refresh_token":"r","access_token":"a","expires_in":3600}""")
        runBlocking { source.login("code") } shouldBe true
        harness.takeRequest().target shouldBe "/realms/mangadex/protocol/openid-connect/token"
        fixture.interceptor.token shouldBe "a"
        harness.enqueue("")
        runBlocking { source.logout() } shouldBe true
        harness.takeRequest().target shouldBe "/realms/mangadex/protocol/openid-connect/logout"
    }

    @Test
    fun followStatusAndRating() {
        harness.answer { """{"result":"ok"}""" }
        runBlocking { source.updateFollowStatus("m1", FollowStatus.READING) } shouldBe true
        val track = Track.create(TrackerManager.MDLIST).apply {
            trackingUrl = "/manga/m1"
            score = 5.0
        }
        runBlocking { source.updateRating(track) } shouldBe true
    }

    @Test
    fun trackingInfo() {
        harness.answer { target ->
            if (target.endsWith("/status")) """{"status":"reading"}""" else """{"ratings":{}}"""
        }
        val track = runBlocking { source.fetchTrackingInfo("/manga/m1") }
        track.status shouldBe FollowStatus.READING.long
        track.score shouldBe 0.0
    }

    @Test
    fun similarAndRelated() {
        harness.answer { target ->
            when {
                target.startsWith("/similar/") ->
                    """{"id":"m0","title":{},"contentRating":"safe","updatedAt":"n","matches":[]}"""
                target.startsWith("/manga/m0/relation") -> """{"response":"collection","data":[]}"""
                target.startsWith("/manga?") -> """{"limit":0,"offset":0,"total":0,"data":[]}"""
                else -> null
            }
        }
        runBlocking { source.getMangaSimilar(sManga("/manga/m0")) }.mangas.isEmpty() shouldBe true
        runBlocking { source.getMangaRelated(sManga("/manga/m0")) }.mangas.isEmpty() shouldBe true
    }

    @Test
    fun metadataFromTrack() {
        harness.answer { target -> if (target.startsWith("/manga/m1")) MD_MANGA_JSON else null }
        val track = Track.create(TrackerManager.MDLIST).apply { trackingUrl = "https://mangadex.org/title/m1" }
        runBlocking { source.getMangaMetadata(track) }.title shouldBe "Title"
        SAMPLE_DATA_JSON.contains("m1") shouldBe true
    }

    @Test
    fun preferencesDefaults() {
        source.dataSaver() shouldBe false
        source.usePort443Only() shouldBe false
        source.blockedGroups() shouldBe ""
        source.blockedUploaders() shouldBe ""
        source.coverQuality() shouldBe ""
        source.tryUsingFirstVolumeCover() shouldBe false
        source.altTitlesInDesc() shouldBe false
        source.finalChapterInDesc() shouldBe false
        source.preferExtensionLangTitle() shouldBe true
        source.detailsPreferences().preferExtensionLangTitle shouldBe true
    }

    @Test
    fun preferencesFromStore() {
        source.sourcePreferences.edit()
            .putBoolean("dataSaverV5_en", true)
            .putBoolean("usePort443_en", true)
            .putString("blockedGroups_en", "g1")
            .putString("blockedUploader_en", "u1")
            .putString("thumbnailQuality_en", ".512.jpg")
            .putBoolean("tryUsingFirstVolumeCover_en", true)
            .putBoolean("altTitlesInDesc_en", true)
            .putBoolean("finalChapterInDesc_en", true)
            .putBoolean("preferExtensionLangTitle_en", false)
            .commit()
        source.dataSaver() shouldBe true
        source.usePort443Only() shouldBe true
        source.blockedGroups() shouldBe "g1"
        source.blockedUploaders() shouldBe "u1"
        val preferences = source.detailsPreferences()
        preferences.coverQuality shouldBe ".512.jpg"
        preferences.tryUsingFirstVolumeCover shouldBe true
        preferences.altTitlesInDesc shouldBe true
        preferences.finalChapterInDesc shouldBe true
        preferences.preferExtensionLangTitle shouldBe false
    }

    @Test
    fun handlersAreBuiltOnce() {
        val handlers = MangaDexHandlers(source, harness.networkHelper)
        (handlers.mangadexService === handlers.mangadexService) shouldBe true
        handlers.mangadexAuthService.client shouldBe source.baseHttpClient
        handlers.similarService.javaClass.simpleName shouldBe "SimilarService"
        handlers.apiMangaParser.metaClass.simpleName shouldBe "MangaDexSearchMetadata"
        handlers.followsHandler.javaClass.simpleName shouldBe "FollowsHandler"
        handlers.mangaHandler.javaClass.simpleName shouldBe "MangaHandler"
        handlers.similarHandler.javaClass.simpleName shouldBe "SimilarHandler"
        handlers.mangaPlusHandler.headers["Origin"] shouldBe "https://mangaplus.shueisha.co.jp"
        handlers.comikeyHandler.headers["User-Agent"] shouldBe SourceTestHarness.USER_AGENT
        handlers.bilibiliHandler.baseUrl shouldBe "https://www.bilibilicomics.com"
        handlers.azukHandler.headers["User-Agent"] shouldBe SourceTestHarness.USER_AGENT
        handlers.mangaHotHandler.headers["User-Agent"] shouldBe SourceTestHarness.USER_AGENT
        handlers.namicomiHandler.client shouldBe harness.client
        val page = eu.kanade.tachiyomi.source.model.Page(0, "u", "https://azuki/1")
        handlers.pageHandler.getImageCall(page, 0L)?.request()?.url?.host shouldBe "azuki"
    }
}
