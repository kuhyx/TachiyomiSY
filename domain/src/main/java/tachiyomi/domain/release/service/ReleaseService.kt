package tachiyomi.domain.release.service

import tachiyomi.domain.release.model.Release

/** The GitHub releases API, as far as the update check needs it. */
public interface ReleaseService {

    /** The latest release of the GitHub `owner/repo` [repository]; throws when the request fails. */
    public suspend fun latest(repository: String): Release
}
