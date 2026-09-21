package eu.kanade.tachiyomi.data.track.komga

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

/** The [Komga] tracker: vocabulary, the enhanced-tracker contract and progress sync. */
internal class KomgaTest {

    private val harness = KomgaHarness()
    private val tracker: Komga
        get() = harness.tracker
    private val manga = Manga.create().copy(url = KomgaHarness.SERIES_URL)

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
        tracker.id shouldBe 6L
        tracker.name shouldBe "Komga"
        tracker.getLogo() shouldBe R.drawable.brand_komga
        tracker.getStatusList() shouldBe listOf(1L, 2L, 3L)
        tracker.getStatus(Komga.UNREAD) shouldBe MR.strings.unread
        tracker.getStatus(Komga.READING) shouldBe MR.strings.reading
        tracker.getStatus(Komga.COMPLETED) shouldBe MR.strings.completed
        tracker.getStatus(9L).shouldBeNull()
        tracker.getReadingStatus() shouldBe Komga.READING
        tracker.getRereadingStatus() shouldBe -1L
        tracker.getCompletionStatus() shouldBe Komga.COMPLETED
        tracker.getScoreList() shouldBe emptyList()
        tracker.displayScore(domainTrack(score = 3.0)) shouldBe ""
        tracker.getAcceptedSources() shouldBe listOf("eu.kanade.tachiyomi.extension.all.komga.Komga")
        tracker.client.interceptors.size shouldBe 1
    }

    @Test
    fun loginIsANoop() = runTest {
        tracker.login("ignored", "ignored")
        tracker.isLoggedIn shouldBe true
        tracker.logout()
        tracker.loginNoop()
        tracker.getUsername() shouldBe "user"
        tracker.getPassword() shouldBe "pass"
    }

    @Test
    fun bindAndSearch() = runTest {
        val track = dbTrack(trackerId = 6L)
        tracker.bind(track) shouldBe track
        tracker.bind(track, hasReadChapters = true) shouldBe track
        shouldThrow<UnsupportedOperationException> { tracker.search("x") }.message shouldBe
            "Search is not supported by this tracker"
    }

    @Test
    fun sourceAcceptance() {
        val komga = KomgaSource()
        val kavita = KavitaSource()
        tracker.accept(komga) shouldBe true
        tracker.accept(kavita) shouldBe false

        val track = domainTrack(trackerId = 6L, remoteUrl = KomgaHarness.SERIES_URL)
        tracker.isTrackFrom(track, manga, komga) shouldBe true
        tracker.isTrackFrom(track, manga, kavita) shouldBe false
        tracker.isTrackFrom(track, manga, null) shouldBe false
        tracker.isTrackFrom(track.copy(remoteUrl = "other"), manga, komga) shouldBe false

        val moved = manga.copy(url = "http://komga.local/api/v1/series/series-2")
        tracker.migrateTrack(track, moved, komga)?.remoteUrl shouldBe "http://komga.local/api/v1/series/series-2"
        tracker.migrateTrack(track, moved, kavita).shouldBeNull()
    }

    @Test
    fun matchAndRefresh() = runTest {
        harness.enqueue("series.json")
        harness.enqueue("progress_v2.json")
        checkNotNull(tracker.match(manga)).title shouldBe "Komga Title"

        harness.enqueueRaw("", code = 500)
        tracker.match(manga).shouldBeNull()

        harness.enqueue("series.json")
        harness.enqueue("progress_v2.json")
        val track = dbTrack(trackerId = 6L, trackingUrl = KomgaHarness.SERIES_URL)
        tracker.refresh(track) shouldBe track
        track.totalChapters shouldBe 10L
        track.lastChapterRead shouldBe 4.5
        track.status shouldBe Komga.READING
    }

    @Test
    fun updateDerivesTheStatus() = runTest {
        suspend fun update(status: Long, last: Double, total: Long, didRead: Boolean): Long {
            harness.enqueueRaw("")
            harness.enqueue("series.json")
            harness.enqueue("progress_v2.json")
            val track = dbTrack(
                trackerId = 6L,
                status = status,
                lastChapterRead = last,
                totalChapters = total,
                trackingUrl = KomgaHarness.SERIES_URL,
            )
            tracker.update(track, didRead)
            return track.status
        }
        update(status = Komga.UNREAD, last = 1.0, total = 5L, didRead = false) shouldBe Komga.UNREAD
        update(status = Komga.COMPLETED, last = 1.0, total = 5L, didRead = true) shouldBe Komga.COMPLETED
        update(status = Komga.UNREAD, last = 5.0, total = 5L, didRead = true) shouldBe Komga.COMPLETED
        update(status = Komga.UNREAD, last = 2.0, total = 5L, didRead = true) shouldBe Komga.READING
        update(status = Komga.UNREAD, last = 0.0, total = 0L, didRead = true) shouldBe Komga.READING
    }

    @Test
    fun updateWithoutReadUsesDefault() = runTest {
        harness.enqueueRaw("")
        harness.enqueue("series.json")
        harness.enqueue("progress_v2.json")
        val track = dbTrack(trackerId = 6L, status = Komga.UNREAD, trackingUrl = KomgaHarness.SERIES_URL)
        tracker.update(track).lastChapterRead shouldBe 4.5
        track.status shouldBe Komga.UNREAD
    }
}
