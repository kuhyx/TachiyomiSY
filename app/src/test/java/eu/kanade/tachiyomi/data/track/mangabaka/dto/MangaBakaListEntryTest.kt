package eu.kanade.tachiyomi.data.track.mangabaka.dto

import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBaka
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class MangaBakaListEntryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun entry(state: String): MangaBakaListEntry = MangaBakaListEntry(
        state = state,
        startDate = null,
        finishDate = null,
        isPrivate = false,
        progressChapter = null,
        rating = null,
    )

    @Test
    fun decodesFullAndMinimalEntries() {
        val full = json.decodeFromString<MangaBakaListResult>(
            fixture("eu/kanade/tachiyomi/data/track/mangabaka/library_entry.json"),
        ).data
        full.state shouldBe "reading"
        full.startDate shouldBe "2024-01-02T03:04:05Z"
        full.finishDate shouldBe "2024-02-03T04:05:06Z"
        full.isPrivate shouldBe true
        full.progressChapter shouldBe 12.5
        full.rating shouldBe 80L

        val minimal = json.decodeFromString<MangaBakaListResult>(
            fixture("eu/kanade/tachiyomi/data/track/mangabaka/library_entry_minimal.json"),
        ).data
        minimal shouldBe entry("plan_to_read")
        json.decodeFromString<MangaBakaListResult>(json.encodeToString(MangaBakaListResult(minimal))).data shouldBe
            minimal
    }

    @Test
    fun everyStateMapsToAStatus() {
        entry("considering").getStatus() shouldBe MangaBaka.CONSIDERING
        entry("completed").getStatus() shouldBe MangaBaka.COMPLETED
        entry("dropped").getStatus() shouldBe MangaBaka.DROPPED
        entry("paused").getStatus() shouldBe MangaBaka.PAUSED
        entry("plan_to_read").getStatus() shouldBe MangaBaka.PLAN_TO_READ
        entry("reading").getStatus() shouldBe MangaBaka.READING
        entry("rereading").getStatus() shouldBe MangaBaka.REREADING
    }

    @Test
    fun unknownStateThrows() {
        val error = shouldThrow<IllegalArgumentException> { entry("archived").getStatus() }
        error.message shouldBe "Unknown status: archived"
    }
}
