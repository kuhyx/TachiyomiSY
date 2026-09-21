package eu.kanade.tachiyomi.data.track.kavita

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import eu.kanade.tachiyomi.extension.all.kavita.Kavita as KavitaSource
import eu.kanade.tachiyomi.extension.all.komga.Komga as KomgaSource

/** The [Kavita] tracker: vocabulary, the enhanced-tracker contract and source authentication. */
internal class KavitaTest {

    private val harness = KavitaHarness()
    private val tracker: Kavita
        get() = harness.tracker
    private val manga = Manga.create().copy(url = KavitaHarness.SERIES_URL)

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun identityAndVocabulary() {
        tracker.id shouldBe 8L
        tracker.name shouldBe "Kavita"
        tracker.getLogo() shouldBe R.drawable.brand_kavita
        tracker.getStatusList() shouldBe listOf(1L, 2L, 3L)
        tracker.getStatus(Kavita.UNREAD) shouldBe MR.strings.unread
        tracker.getStatus(Kavita.READING) shouldBe MR.strings.reading
        tracker.getStatus(Kavita.COMPLETED) shouldBe MR.strings.completed
        tracker.getStatus(9L).shouldBeNull()
        tracker.getReadingStatus() shouldBe Kavita.READING
        tracker.getRereadingStatus() shouldBe -1L
        tracker.getCompletionStatus() shouldBe Kavita.COMPLETED
        tracker.getScoreList() shouldBe emptyList()
        tracker.displayScore(domainTrack(score = 3.0)) shouldBe ""
        tracker.getAcceptedSources() shouldBe listOf("eu.kanade.tachiyomi.extension.all.kavita.Kavita")
    }

    @Test
    fun loginIsANoop() = runTest {
        tracker.isLoggedIn shouldBe false
        tracker.login("ignored", "ignored")
        tracker.isLoggedIn shouldBe true
        tracker.logout()
        tracker.loginNoop()
        tracker.getUsername() shouldBe "user"
        tracker.getPassword() shouldBe "pass"
    }

    @Test
    fun bindAndSearch() = runTest {
        val track = dbTrack(trackerId = 8L)
        tracker.bind(track) shouldBe track
        tracker.bind(track, hasReadChapters = true) shouldBe track
        shouldThrow<UnsupportedOperationException> { tracker.search("x") }.message shouldBe
            "Search is not supported by this tracker"
    }

    @Test
    fun sourceAcceptance() {
        val kavita = KavitaSource()
        val komga = KomgaSource()
        tracker.accept(kavita) shouldBe true
        tracker.accept(komga) shouldBe false

        val track = domainTrack(trackerId = 8L, remoteUrl = KavitaHarness.SERIES_URL)
        tracker.isTrackFrom(track, manga, kavita) shouldBe true
        tracker.isTrackFrom(track, manga, komga) shouldBe false
        tracker.isTrackFrom(track, manga, null) shouldBe false
        tracker.isTrackFrom(track.copy(remoteUrl = "other"), manga, kavita) shouldBe false

        val moved = manga.copy(url = "http://kavita.local/api/Series/99")
        tracker.migrateTrack(track, moved, kavita)?.remoteUrl shouldBe "http://kavita.local/api/Series/99"
        tracker.migrateTrack(track, moved, komga).shouldBeNull()
    }

    private fun enqueueSeriesLookup() {
        harness.enqueue("series.json")
        harness.enqueue("volumes.json")
        harness.enqueue("chapter.json")
    }

    @Test
    fun matchAndRefresh() = runTest {
        enqueueSeriesLookup()
        val matched = checkNotNull(tracker.match(manga))
        matched.title shouldBe "Kavita Series"
        matched.totalChapters shouldBe 7L

        harness.enqueueRaw("", code = 500)
        tracker.match(manga).shouldBeNull()

        enqueueSeriesLookup()
        val track = dbTrack(trackerId = 8L, trackingUrl = KavitaHarness.SERIES_URL)
        tracker.refresh(track) shouldBe track
        track.totalChapters shouldBe 7L
        track.lastChapterRead shouldBe 7.5
        track.status shouldBe Kavita.READING
    }

    @Test
    fun updateDerivesTheStatus() = runTest {
        suspend fun update(status: Long, last: Double, total: Long, didRead: Boolean): Long {
            harness.enqueueRaw("")
            enqueueSeriesLookup()
            val track = dbTrack(
                trackerId = 8L,
                status = status,
                lastChapterRead = last,
                totalChapters = total,
                trackingUrl = KavitaHarness.SERIES_URL,
            )
            tracker.update(track, didRead)
            return track.status
        }
        update(status = Kavita.UNREAD, last = 1.0, total = 5L, didRead = false) shouldBe Kavita.UNREAD
        update(status = Kavita.COMPLETED, last = 1.0, total = 5L, didRead = true) shouldBe Kavita.COMPLETED
        update(status = Kavita.UNREAD, last = 5.0, total = 5L, didRead = true) shouldBe Kavita.COMPLETED
        update(status = Kavita.UNREAD, last = 2.0, total = 5L, didRead = true) shouldBe Kavita.READING
        update(status = Kavita.UNREAD, last = 0.0, total = 0L, didRead = true) shouldBe Kavita.READING
    }

    @Test
    fun updateWithoutReadUsesDefault() = runTest {
        harness.enqueueRaw("")
        enqueueSeriesLookup()
        val track = dbTrack(trackerId = 8L, status = Kavita.UNREAD, trackingUrl = KavitaHarness.SERIES_URL)
        tracker.update(track).lastChapterRead shouldBe 7.5
        track.status shouldBe Kavita.UNREAD
    }

    @Test
    fun loadOAuthAuthenticatesSources() {
        harness.configureSources("" to "key", KavitaHarness.API_URL to "", KavitaHarness.API_URL to "key")
        harness.enqueue("auth.json")
        tracker.loadOAuth()
        val auths = checkNotNull(tracker.authentications).authentications
        auths[0] shouldBe SourceAuth(sourceId = 1)
        auths[1] shouldBe SourceAuth(sourceId = 2)
        auths[2] shouldBe SourceAuth(sourceId = 3, apiUrl = KavitaHarness.API_URL, jwtToken = "jwt-token")
    }

    @Test
    fun emptyTokensAreDropped() {
        harness.configureSources(KavitaHarness.API_URL to "key", KavitaHarness.API_URL to "key", null to null)
        harness.enqueueRaw("", code = 404)
        harness.enqueueRaw("""{"username":"u","token":"","apiKey":"k"}""")
        tracker.loadOAuth()
        val auths = checkNotNull(tracker.authentications).authentications
        auths[0] shouldBe SourceAuth(sourceId = 1)
        auths[1] shouldBe SourceAuth(sourceId = 2)
        auths[2] shouldBe SourceAuth(sourceId = 3)
    }
}
