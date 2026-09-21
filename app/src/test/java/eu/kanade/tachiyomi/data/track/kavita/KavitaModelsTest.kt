package eu.kanade.tachiyomi.data.track.kavita

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.fixture
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class KavitaModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun load(name: String): String = fixture("eu/kanade/tachiyomi/data/track/kavita/$name")

    @Test
    fun seriesDecodesAndBecomesATrack() {
        val series = json.decodeFromString<SeriesDto>(load("series.json"))
        series.id shouldBe 12
        series.originalName shouldBe "Original"
        series.thumbnailUrl shouldBe "https://kavita.local/thumb.jpg"
        series.localizedName shouldBe "Localized"
        series.sortName shouldBe "kavita series"
        series.pages shouldBe 300
        series.coverImageLocked shouldBe false
        series.pagesRead shouldBe 150
        series.userRating shouldBe 4
        series.userReview shouldBe "good"
        series.format shouldBe 1
        series.created shouldBe "2024-01-01"
        series.libraryId shouldBe 3
        series.libraryName shouldBe "Manga"

        val track = series.toTrack()
        track.trackerId shouldBe TrackerManager.KAVITA
        track.title shouldBe "Kavita Series"
        track.summary shouldBe ""
    }

    @Test
    fun seriesDefaults() {
        val bare = json.decodeFromString<SeriesDto>(load("series_minimal.json"))
        bare shouldBe SeriesDto(id = 13, name = "Bare", pages = 10, pagesRead = 0, format = 0, libraryId = 1)
        bare.originalName shouldBe ""
        bare.thumbnailUrl shouldBe ""
        bare.coverImageLocked shouldBe true
        bare.userRating shouldBe 0
        bare.libraryName shouldBe ""
        json.decodeFromString<SeriesDto>(json.encodeToString(bare)) shouldBe bare
    }

    @Test
    fun volumesAndChapters() {
        val volumes = json.decodeFromString<List<VolumeDto>>(load("volumes.json"))
        volumes.size shouldBe 3
        val second = volumes[1]
        second.id shouldBe 2
        second.number shouldBe 2
        second.name shouldBe "Volume 2"
        second.pages shouldBe 100
        second.pagesRead shouldBe 50
        second.lastModified shouldBe "2024-01-01"
        second.created shouldBe "2024-01-01"
        second.seriesId shouldBe 12

        val full = second.chapters[1]
        full shouldBe ChapterDto(
            id = 12,
            range = "7.5",
            number = "7.5",
            pages = 20,
            isSpecial = false,
            title = "t",
            pagesRead = 1,
            coverImageLocked = true,
            volumeId = 2,
            created = "c",
        )
        second.chapters.size shouldBe 3
        val bare = volumes[2].chapters.single()
        bare.id shouldBe 13
        bare.range shouldBe ""
        bare.number shouldBe "3"
        bare.pages shouldBe 0
        bare.isSpecial shouldBe false
        bare.title shouldBe ""
        bare.pagesRead shouldBe 0
        bare.coverImageLocked shouldBe false
        bare.volumeId shouldBe -1
        bare.created shouldBe ""
        ChapterDto().number shouldBe "-1"
        val volume = VolumeDto(
            id = 1,
            number = 1,
            name = "n",
            pages = 0,
            pagesRead = 0,
            lastModified = "",
            created = "",
            seriesId = 1,
        )
        volume.chapters shouldBe emptyList()
    }

    @Test
    fun authenticationDecodes() {
        val auth = json.decodeFromString<AuthenticationDto>(load("auth.json"))
        auth shouldBe AuthenticationDto(username = "reader", token = "jwt-token", apiKey = "api-key")
        auth.username shouldBe "reader"
        auth.token shouldBe "jwt-token"
        auth.apiKey shouldBe "api-key"
    }

    @Test
    fun oauthLooksUpTokensByApiUrl() {
        val empty = OAuth()
        empty.authentications.map { it.sourceId } shouldBe listOf(1, 2, 3)
        empty.authentications.first() shouldBe SourceAuth(sourceId = 1, apiUrl = "", jwtToken = "")
        empty.getToken("http://kavita.local/api") shouldBe null
        empty.getToken("") shouldBe ""

        val configured = OAuth(
            listOf(
                SourceAuth(sourceId = 1),
                SourceAuth(sourceId = 2, apiUrl = "http://a/api", jwtToken = "jwt-a"),
                SourceAuth(sourceId = 3, apiUrl = "http://b/api", jwtToken = "jwt-b"),
            ),
        )
        configured.getToken("http://b/api") shouldBe "jwt-b"
        configured.getToken("http://c/api") shouldBe null
        SourceAuth(sourceId = 2, apiUrl = "http://a/api", jwtToken = "jwt-a").toString() shouldBe
            "SourceAuth(sourceId=2, apiUrl=http://a/api, jwtToken=jwt-a)"
    }
}
