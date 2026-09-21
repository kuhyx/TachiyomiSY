package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MyAnimeListUtilsTest {

    private fun status(value: Long) = dbTrack(TrackerManager.MYANIMELIST, status = value).toMyAnimeListStatus()

    @Test
    fun trackStatusToApi() {
        status(MyAnimeList.READING) shouldBe "reading"
        status(MyAnimeList.COMPLETED) shouldBe "completed"
        status(MyAnimeList.ON_HOLD) shouldBe "on_hold"
        status(MyAnimeList.DROPPED) shouldBe "dropped"
        status(MyAnimeList.PLAN_TO_READ) shouldBe "plan_to_read"
        status(MyAnimeList.REREADING) shouldBe "reading"
        status(42L).shouldBeNull()
    }

    @Test
    fun apiStatusToTrack() {
        getStatus("reading") shouldBe MyAnimeList.READING
        getStatus("completed") shouldBe MyAnimeList.COMPLETED
        getStatus("on_hold") shouldBe MyAnimeList.ON_HOLD
        getStatus("dropped") shouldBe MyAnimeList.DROPPED
        getStatus("plan_to_read") shouldBe MyAnimeList.PLAN_TO_READ
        getStatus("rewatching") shouldBe MyAnimeList.READING
        getStatus(null) shouldBe MyAnimeList.READING
    }
}
