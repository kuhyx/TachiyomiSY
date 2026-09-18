package tachiyomi.domain.release.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService
import java.time.Instant

internal class GetApplicationReleaseTest {

    lateinit var getApplicationRelease: GetApplicationRelease
    lateinit var releaseService: ReleaseService
    lateinit var preference: Preference<Long>

    @BeforeEach
    fun beforeEach() {
        val preferenceStore = mockk<PreferenceStore>()
        preference = mockk()
        every { preferenceStore.getLong(any(), any()) } returns preference
        releaseService = mockk()

        getApplicationRelease = GetApplicationRelease(releaseService, preferenceStore)
    }

    @Test
    fun `preview update is reported`() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        val release = Release(
            "200",
            "info",
            "http://example.com/release_link",
            listOf("http://example.com/assets"),
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isPreview = true,
                commitCount = 1000,
                versionName = "",
                repository = "test",
                syDebugVersion = "100",
            ),
        )

        (result as GetApplicationRelease.Result.NewUpdate).release shouldBe GetApplicationRelease.Result.NewUpdate(
            release,
        ).release
    }

    @Test
    fun `update is reported`() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        val release = Release(
            "v2.0.0",
            "info",
            "http://example.com/release_link",
            listOf("http://example.com/assets"),
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isPreview = false,
                commitCount = 0,
                versionName = "v1.0.0",
                syDebugVersion = "0",
                repository = "test",
            ),
        )

        (result as GetApplicationRelease.Result.NewUpdate).release shouldBe GetApplicationRelease.Result.NewUpdate(
            release,
        ).release
    }

    @Test
    fun `no update is not reported`() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        val release = Release(
            "v1.0.0",
            "info",
            "http://example.com/release_link",
            listOf("http://example.com/assets"),
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isPreview = false,
                commitCount = 0,
                versionName = "v2.0.0",
                syDebugVersion = "0",
                repository = "test",
            ),
        )

        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    @Test
    fun `no check before three days`() = runTest {
        every { preference.get() } returns Instant.now().toEpochMilli()
        every { preference.set(any()) }.answers { }

        val release = Release(
            "v1.0.0",
            "info",
            "http://example.com/release_link",
            listOf("http://example.com/assets"),
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isPreview = false,
                commitCount = 0,
                versionName = "v2.0.0",
                syDebugVersion = "0",
                repository = "test",
            ),
        )

        coVerify(exactly = 0) { releaseService.latest(any()) }
        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    @Test
    fun forceCheckIgnoresInterval() = runTest {
        every { preference.get() } returns Instant.now().toEpochMilli()
        every { preference.set(any()) }.answers { }

        val release = Release(
            version = "v2.0.0",
            info = "info",
            releaseLink = "http://example.com/release_link",
            assets = listOf("http://example.com/assets"),
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isPreview = false,
                commitCount = 0,
                versionName = "v1.0.0",
                repository = "test",
                syDebugVersion = "0",
                forceCheck = true,
            ),
        )

        coVerify(exactly = 1) { releaseService.latest("test") }
        result shouldBe GetApplicationRelease.Result.NewUpdate(release)
    }

    @Test
    fun previewWithoutBuildNumber() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        coEvery { releaseService.latest(any()) } returns Release(
            version = "200",
            info = "info",
            releaseLink = "http://example.com/release_link",
            assets = listOf("http://example.com/assets"),
        )

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isPreview = true,
                commitCount = 0,
                versionName = "",
                repository = "test",
                syDebugVersion = "debug",
            ),
        )

        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    @Test
    fun previewAlreadyCurrent() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        coEvery { releaseService.latest(any()) } returns Release(
            version = "r200",
            info = "info",
            releaseLink = "http://example.com/release_link",
            assets = listOf("http://example.com/assets"),
        )

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isPreview = true,
                commitCount = 0,
                versionName = "",
                repository = "test",
                syDebugVersion = "200",
            ),
        )

        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }
}
