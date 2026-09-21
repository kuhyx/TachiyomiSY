package eu.kanade.tachiyomi.data.track.hikka.dto

import eu.kanade.tachiyomi.data.track.fixture
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/** [HKAuthTokenInfo] and the nested [HKClient] and [HKUser]. */
internal class HKAuthTokenInfoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesTheFixture() {
        val info = json.decodeFromString<HKAuthTokenInfo>(
            fixture("eu/kanade/tachiyomi/data/track/hikka/token_info.json"),
        )
        info.reference shouldBe "token-ref"
        info.created shouldBe 1_700_000_000L
        info.scope shouldBe listOf("readlist", "read:user-details")
        info.expiration shouldBe 4_102_444_800L
        info.used shouldBe 1_700_000_500L

        val client = info.client
        client.reference shouldBe "client-ref"
        client.name shouldBe "TachiyomiSY"
        client.description shouldBe "tracker"
        client.verified shouldBe true
        client.created shouldBe 1_600_000_000L
        client.updated shouldBe 1_600_000_001L
        client.user shouldBe HKUser(reference = "owner-ref", username = "owner")
        client.user.reference shouldBe "owner-ref"
        client.user.username shouldBe "owner"

        json.decodeFromString<HKAuthTokenInfo>(json.encodeToString(info)) shouldBe info
        val built = HKAuthTokenInfo(
            reference = "token-ref",
            created = 1_700_000_000L,
            client = HKClient(
                reference = "client-ref",
                name = "TachiyomiSY",
                description = "tracker",
                verified = true,
                user = HKUser(reference = "owner-ref", username = "owner"),
                created = 1_600_000_000L,
                updated = 1_600_000_001L,
            ),
            scope = listOf("readlist", "read:user-details"),
            expiration = 4_102_444_800L,
            used = 1_700_000_500L,
        )
        built shouldBe info
    }

    @Test
    fun userDecodesAlone() {
        val user = json.decodeFromString<HKUser>(fixture("eu/kanade/tachiyomi/data/track/hikka/user.json"))
        user shouldBe HKUser("user-ref", "hikka_user")
        user.copy(username = "x").toString() shouldBe "HKUser(reference=user-ref, username=x)"
    }
}
