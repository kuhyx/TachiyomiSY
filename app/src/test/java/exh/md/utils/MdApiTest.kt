package exh.md.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MdApiTest {
    @Test
    fun endpointsDeriveFromBase() {
        MdApi.manga shouldBe "https://api.mangadex.org/manga"
        MdApi.cover shouldBe "https://api.mangadex.org/cover"
        MdApi.chapter shouldBe "https://api.mangadex.org/chapter"
        MdApi.group shouldBe "https://api.mangadex.org/group"
        MdApi.author shouldBe "https://api.mangadex.org/author"
        MdApi.rating shouldBe "https://api.mangadex.org/rating"
        MdApi.statistics shouldBe "https://api.mangadex.org/statistics/manga"
        MdApi.chapterImageServer shouldBe "https://api.mangadex.org/at-home/server"
        MdApi.userFollows shouldBe "https://api.mangadex.org/user/follows/manga"
        MdApi.readingStatusForAllManga shouldBe "https://api.mangadex.org/manga/status"
        MdApi.atHomeServer shouldBe "https://api.mangadex.org/at-home/server"
        MdApi.legacyMapping shouldBe "https://api.mangadex.org/legacy/mapping"
        MdApi.login shouldBe "/realms/mangadex/protocol/openid-connect/auth"
        MdApi.logout shouldBe "/realms/mangadex/protocol/openid-connect/logout"
        MdApi.token shouldBe "/realms/mangadex/protocol/openid-connect/token"
        MdApi.userInfo shouldBe "/realms/mangadex/protocol/openid-connect/userinfo"
    }
}
