package eu.kanade.tachiyomi.data.track.anilist.dto

import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** The plain response envelopes: decode each fixture, read every field back and re-encode it. */
internal class ALDtoTest {

    private val json = TrackerHarness.json

    private inline fun <reified T> roundTrip(name: String): T {
        val decoded = json.decodeFromString<T>(fixture("anilist", name))
        exerciseDto(decoded)
        json.decodeFromString<T>(json.encodeToString(decoded)) shouldBe decoded
        return decoded
    }

    @Test
    fun addMangaResult() {
        roundTrip<ALAddMangaResult>("add_manga.json") shouldBe ALAddMangaResult(ALAddMangaData(ALAddMangaEntry(5001L)))
    }

    @Test
    fun currentUserResult() {
        roundTrip<ALCurrentUserResult>("current_user.json") shouldBe
            ALCurrentUserResult(ALUserViewer(ALUserViewerData(777, "kuhy", ALUserListOptions("POINT_100"))))
    }

    @Test
    fun mangaMetadata() {
        val decoded = roundTrip<ALMangaMetadata>("metadata.json")
        decoded.data.media.id shouldBe 101L
        decoded.data.media.staff.edges.size shouldBe 4
        decoded.data.media.staff.edges[0] shouldBe ALEdge("Story & Art", 1, ALStaffNode(ALStaffName("Both")))
    }

    @Test
    fun userListResult() {
        val decoded = roundTrip<ALUserListMangaQueryResult>("user_list.json")
        val item = decoded.data.page.mediaList.single()
        item.id shouldBe 5001L
        item.status shouldBe "CURRENT"
        item.scoreRaw shouldBe 80
        item.progress shouldBe 12
        item.private shouldBe true
        roundTrip<ALUserListMangaQueryResult>("user_list_empty.json").data.page.mediaList shouldBe emptyList()
    }

    @Test
    fun searchResult() {
        roundTrip<ALSearchResult>("search.json").data.page.media.size shouldBe 4
        roundTrip<ALIdSearchResult>("search_by_id.json").data.media.title shouldBe ALItemTitle("Taiwan Manhua")
    }
}
