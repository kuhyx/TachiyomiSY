package eu.kanade.tachiyomi.data.track.suwayomi

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.bodyText
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import eu.kanade.tachiyomi.extension.all.komga.Komga as KomgaSource
import eu.kanade.tachiyomi.extension.all.tachidesk.Tachidesk as TachideskSource

/** The [Suwayomi] tracker: vocabulary, the enhanced-tracker contract and progress sync. */
internal class SuwayomiTest {

    private val harness = SuwayomiHarness()
    private val tracker: Suwayomi
        get() = harness.tracker
    private val manga = Manga.create().copy(url = "/manga/42")

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
        tracker.id shouldBe 9L
        tracker.name shouldBe "Suwayomi"
        tracker.getLogo() shouldBe R.drawable.brand_suwayomi
        tracker.getStatusList() shouldBe listOf(1L, 2L, 3L)
        tracker.getStatus(Suwayomi.UNREAD) shouldBe MR.strings.unread
        tracker.getStatus(Suwayomi.READING) shouldBe MR.strings.reading
        tracker.getStatus(Suwayomi.COMPLETED) shouldBe MR.strings.completed
        tracker.getStatus(9L).shouldBeNull()
        tracker.getReadingStatus() shouldBe Suwayomi.READING
        tracker.getRereadingStatus() shouldBe -1L
        tracker.getCompletionStatus() shouldBe Suwayomi.COMPLETED
        tracker.getScoreList() shouldBe emptyList()
        tracker.displayScore(domainTrack(score = 3.0)) shouldBe ""
        tracker.getAcceptedSources() shouldBe listOf("eu.kanade.tachiyomi.extension.all.tachidesk.Tachidesk")
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
        val track = dbTrack(trackerId = 9L)
        tracker.bind(track) shouldBe track
        tracker.bind(track, hasReadChapters = true) shouldBe track
        shouldThrow<UnsupportedOperationException> { tracker.search("x") }.message shouldBe
            "Search is not supported by this tracker"
    }

    @Test
    fun sourceAcceptance() {
        val tachidesk = TachideskSource()
        val komga = KomgaSource()
        tracker.accept(tachidesk) shouldBe true
        tracker.accept(komga) shouldBe false

        val track = domainTrack(trackerId = 9L, remoteUrl = "/manga/42")
        tracker.isTrackFrom(track, manga, tachidesk) shouldBe true
        tracker.isTrackFrom(track, manga, komga) shouldBe false
        tracker.isTrackFrom(track, manga, null) shouldBe false
        tracker.isTrackFrom(track.copy(remoteUrl = "/manga/1"), manga, tachidesk) shouldBe false

        val moved = manga.copy(url = "/manga/99")
        tracker.migrateTrack(track, moved, tachidesk)?.remoteUrl shouldBe "/manga/99"
        tracker.migrateTrack(track, moved, komga).shouldBeNull()
    }

    @Test
    fun matchAndRefresh() = runTest {
        harness.enqueue("manga.json")
        val matched = checkNotNull(tracker.match(manga))
        matched.title shouldBe "Suwayomi Manga"
        harness.takeRequest().bodyText() shouldContain """"variables":{"mangaId":42}"""

        harness.enqueueRaw("", code = 500)
        tracker.match(manga).shouldBeNull()
        tracker.match(manga.copy(url = "/manga/not-a-number")).shouldBeNull()

        harness.enqueue("manga.json")
        val track = dbTrack(trackerId = 9L, remoteId = 42L)
        tracker.refresh(track) shouldBe track
        track.totalChapters shouldBe 20L
        track.lastChapterRead shouldBe 12.5
        track.status shouldBe Suwayomi.READING
    }

    private fun enqueueUpdateRound() {
        harness.enqueue("unread_chapters.json")
        harness.enqueueRaw("{}")
        harness.enqueueRaw("{}")
        harness.enqueue("manga.json")
    }

    @Test
    fun updateDerivesTheStatus() = runTest {
        suspend fun update(status: Long, last: Double, total: Long, didRead: Boolean): Long {
            enqueueUpdateRound()
            val track = dbTrack(trackerId = 9L, remoteId = 42L, status = status, lastChapterRead = last)
            track.totalChapters = total
            tracker.update(track, didRead)
            return track.status
        }
        update(status = Suwayomi.UNREAD, last = 1.0, total = 5L, didRead = false) shouldBe Suwayomi.UNREAD
        update(status = Suwayomi.COMPLETED, last = 1.0, total = 5L, didRead = true) shouldBe Suwayomi.COMPLETED
        update(status = Suwayomi.UNREAD, last = 5.0, total = 5L, didRead = true) shouldBe Suwayomi.COMPLETED
        update(status = Suwayomi.UNREAD, last = 2.0, total = 5L, didRead = true) shouldBe Suwayomi.READING
        update(status = Suwayomi.UNREAD, last = 0.0, total = 0L, didRead = true) shouldBe Suwayomi.READING
    }

    @Test
    fun updateHonoursDeletePreference() = runTest {
        enqueueUpdateRound()
        val track = dbTrack(trackerId = 9L, remoteId = 42L, status = Suwayomi.UNREAD, lastChapterRead = 12.5)
        tracker.update(track).lastChapterRead shouldBe 12.5
        harness.takeRequest()
        harness.takeRequest().bodyText().contains("deleteDownloadedChapters") shouldBe false

        every { harness.preferences.getBoolean("Tracker Delete", false) } returns true
        enqueueUpdateRound()
        tracker.update(track)
        repeat(3) { harness.takeRequest() }
        harness.takeRequest().bodyText() shouldContain "deleteDownloadedChapters"
    }
}
