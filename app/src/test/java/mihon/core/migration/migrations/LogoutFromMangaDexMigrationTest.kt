package mihon.core.migration.migrations

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class LogoutFromMangaDexMigrationTest {

    private val migration = LogoutFromMangaDexMigration()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 45f
    }

    @Test
    fun succeedsWithoutTrackers() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe true
    }

    @Test
    fun logsOutOfMdList() = runTest {
        val tracker = mockk<MdList>(relaxed = true)
        val manager = mockk<TrackerManager> { every { mdList } returns tracker }
        startMigrationKoin { single { manager } }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { tracker.logout() }
    }
}
