package mihon.core.migration.migrations

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class LogoutFromMALMigrationTest {

    private val migration = LogoutFromMALMigration()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 12f
    }

    @Test
    fun succeedsWithoutTrackers() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe true
    }

    @Test
    fun logsOutOfMyAnimeList() = runTest {
        val tracker = mockk<MyAnimeList>(relaxed = true)
        val manager = mockk<TrackerManager> { every { myAnimeList } returns tracker }
        startMigrationKoin { single { manager } }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { tracker.logout() }
    }
}
