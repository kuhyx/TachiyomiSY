package tachiyomi.data.release

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tachiyomi.domain.release.model.Release

/**
 * Contains information about the latest release from GitHub.
 *
 * @property version The release tag name.
 * @property info The release notes in GitHub Markdown.
 * @property releaseLink Url of the release page.
 * @property assets The files attached to the release.
 */
@Serializable
public data class GithubRelease(
    @SerialName("tag_name") val version: String,
    @SerialName("body") val info: String,
    @SerialName("html_url") val releaseLink: String,
    @SerialName("assets") val assets: List<GitHubAssets>,
)

/**
 * Assets class containing download url.
 *
 * @property downloadLink Direct download url of the asset.
 */
@Serializable
public data class GitHubAssets(@SerialName("browser_download_url") val downloadLink: String)

/**
 * Regular expression that matches a mention to a valid GitHub username, like it's
 * done in GitHub Flavored Markdown. It follows these constraints:
 *
 * - Alphanumeric with single hyphens (no consecutive hyphens)
 * - Cannot begin or end with a hyphen
 * - Max length of 39 characters
 *
 * Reference: https://stackoverflow.com/a/30281147
 */
public val gitHubUsernameMentionRegex: Regex =
    """\B@([a-z0-9](?:-(?=[a-z0-9])|[a-z0-9]){0,38}(?<=[a-z0-9]))""".toRegex(
        RegexOption.IGNORE_CASE,
    )

/**
 * The domain [Release] for a GitHub release, with `@user` mentions in the notes turned into profile
 * links and the assets reduced to their download urls.
 */
public val releaseMapper: (GithubRelease) -> Release = {
    Release(
        it.version,
        it.info.replace(gitHubUsernameMentionRegex) { mention ->
            "[${mention.value}](https://github.com/${mention.value.substring(1)})"
        },
        it.releaseLink,
        it.assets.map(GitHubAssets::downloadLink),
    )
}
