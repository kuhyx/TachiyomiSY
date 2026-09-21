package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ShikimoriUtilsTest {

    private val statuses = mapOf(
        Shikimori.READING to "watching",
        Shikimori.COMPLETED to "completed",
        Shikimori.ON_HOLD to "on_hold",
        Shikimori.DROPPED to "dropped",
        Shikimori.PLAN_TO_READ to "planned",
        Shikimori.REREADING to "rewatching",
    )

    @Test
    fun trackStatusToApi() {
        statuses.forEach { (status, api) ->
            dbTrack(TrackerManager.SHIKIMORI, status = status).toShikimoriStatus() shouldBe api
        }
    }

    @Test
    fun unknownTrackStatusFails() {
        shouldThrow<IllegalArgumentException> { dbTrack(TrackerManager.SHIKIMORI, status = 42L).toShikimoriStatus() }
    }

    @Test
    fun apiStatusToTrack() {
        statuses.forEach { (status, api) -> toTrackStatus(api) shouldBe status }
    }

    @Test
    fun unknownApiStatusFails() {
        shouldThrow<IllegalArgumentException> { toTrackStatus("reading") }
    }
}
