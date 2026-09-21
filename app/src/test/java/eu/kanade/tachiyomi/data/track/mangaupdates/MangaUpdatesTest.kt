package eu.kanade.tachiyomi.data.track.mangaupdates

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class MangaUpdatesTest {

    private lateinit var mangaUpdates: MangaUpdates

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        mangaUpdates = MangaUpdates(TrackerManager.MANGAUPDATES)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun identityAndFeatureFlags() {
        mangaUpdates.id shouldBe TrackerManager.MANGAUPDATES
        mangaUpdates.name shouldBe "MangaUpdates"
        mangaUpdates.getLogo() shouldBe R.drawable.brand_mangaupdates
        mangaUpdates.supportsReadingDates shouldBe false
        mangaUpdates.supportsPrivateTracking shouldBe false
    }

    @Test
    fun statusVocabulary() {
        mangaUpdates.getStatusList() shouldBe listOf(0L, 2L, 4L, 3L, 1L)
        mangaUpdates.getReadingStatus() shouldBe MangaUpdates.READING_LIST
        mangaUpdates.getRereadingStatus() shouldBe -1L
        mangaUpdates.getCompletionStatus() shouldBe MangaUpdates.COMPLETE_LIST
    }

    @Test
    fun statusStringsForEveryStatus() {
        mangaUpdates.getStatus(MangaUpdates.READING_LIST) shouldBe MR.strings.reading_list
        mangaUpdates.getStatus(MangaUpdates.WISH_LIST) shouldBe MR.strings.wish_list
        mangaUpdates.getStatus(MangaUpdates.COMPLETE_LIST) shouldBe MR.strings.complete_list
        mangaUpdates.getStatus(MangaUpdates.ON_HOLD_LIST) shouldBe MR.strings.on_hold_list
        mangaUpdates.getStatus(MangaUpdates.UNFINISHED_LIST) shouldBe MR.strings.unfinished_list
        mangaUpdates.getStatus(42L).shouldBeNull()
    }

    @Test
    fun scoreListHasTenthSteps() {
        val scores = mangaUpdates.getScoreList()
        scores.size shouldBe 92
        scores.first() shouldBe "-"
        scores[1] shouldBe "1.0"
        scores[10] shouldBe "1.9"
        scores.last() shouldBe "10.0"
    }

    @Test
    fun indexToScoreReadsTheList() {
        mangaUpdates.indexToScore(0) shouldBe 0.0
        mangaUpdates.indexToScore(1) shouldBe 1.0
        mangaUpdates.indexToScore(91) shouldBe 10.0
        mangaUpdates.displayScore(domainTrack(TrackerManager.MANGAUPDATES, score = 8.5)) shouldBe "8.5"
    }

    @Test
    fun sessionComesFromPassword() {
        mangaUpdates.restoreSession().shouldBeNull()
        mangaUpdates.saveCredentials("555", "sess")
        mangaUpdates.restoreSession() shouldBe "sess"
    }
}
