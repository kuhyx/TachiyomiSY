package tachiyomi.data.release

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import mihon.data.extension.assertDataClass
import org.junit.jupiter.api.Test
import tachiyomi.domain.release.model.Release

internal class GithubReleaseTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val release = GithubRelease(
        version = "v1.2.3",
        info = "Thanks @kuhy and @some-user!",
        releaseLink = "https://github.com/o/r/releases/tag/v1.2.3",
        assets = listOf(GitHubAssets("https://github.com/o/r/a.apk"), GitHubAssets("https://github.com/o/r/b.apk")),
    )

    @Test
    fun mapperLinksMentionsAndAssets() {
        releaseMapper(release) shouldBe Release(
            version = "v1.2.3",
            info = "Thanks [@kuhy](https://github.com/kuhy) and [@some-user](https://github.com/some-user)!",
            releaseLink = "https://github.com/o/r/releases/tag/v1.2.3",
            assets = listOf("https://github.com/o/r/a.apk", "https://github.com/o/r/b.apk"),
        )
    }

    @Test
    fun mapperLeavesPlainNotes() {
        val plain = release.copy(info = "mail me at me@example.com", assets = emptyList())

        val mapped = releaseMapper(plain)

        mapped.info shouldBe "mail me at me@example.com"
        mapped.assets.shouldBeEmpty()
    }

    @Test
    fun mentionRegexGithubRules() {
        gitHubUsernameMentionRegex.find("@a-b")?.value shouldBe "@a-b"
        gitHubUsernameMentionRegex.find("@Mixed9")?.value shouldBe "@Mixed9"
        gitHubUsernameMentionRegex.find("@a--b")?.value shouldBe "@a"
        gitHubUsernameMentionRegex.find("@-lead") shouldBe null
        gitHubUsernameMentionRegex.find("@trail-")?.value shouldBe "@trail"
        gitHubUsernameMentionRegex.find("@" + "x".repeat(45))?.value shouldBe "@" + "x".repeat(39)
    }

    @Test
    fun decodesGithubPayload() {
        val payload = """{"tag_name":"v1.2.3","body":"Thanks @kuhy and @some-user!","draft":false,""" +
            """"html_url":"https://github.com/o/r/releases/tag/v1.2.3",""" +
            """"assets":[{"browser_download_url":"https://github.com/o/r/a.apk","size":1},""" +
            """{"browser_download_url":"https://github.com/o/r/b.apk"}]}"""

        json.decodeFromString(GithubRelease.serializer(), payload) shouldBe release
    }

    @Test
    fun encodesWithGithubNames() {
        val encoded = json.encodeToString(GithubRelease.serializer(), release)

        encoded.contains("\"tag_name\"") shouldBe true
        encoded.contains("\"browser_download_url\"") shouldBe true
        json.decodeFromString(GithubRelease.serializer(), encoded) shouldBe release
    }

    @Test
    fun dataClassContracts() {
        val asset = GitHubAssets("https://github.com/o/r/a.apk")

        assertDataClass(
            value = release,
            equal = release.copy(),
            different = listOf(
                release.copy(version = "x"),
                release.copy(info = "x"),
                release.copy(releaseLink = "x"),
                release.copy(assets = emptyList()),
            ),
        )
        assertDataClass(value = asset, equal = asset.copy(), different = listOf(asset.copy(downloadLink = "x")))
    }
}
