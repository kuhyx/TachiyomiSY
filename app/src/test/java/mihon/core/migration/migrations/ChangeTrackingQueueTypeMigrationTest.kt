package mihon.core.migration.migrations

import android.content.Context
import androidx.core.content.edit
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ChangeTrackingQueueTypeMigrationTest {

    private val migration = ChangeTrackingQueueTypeMigration()
    private val app = robolectricApp()
    private val queue = app.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 44f
    }

    @Test
    fun failsWithoutApplication() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun copesWithAnEmptyQueue() = runTest {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        queue.all.isEmpty() shouldBe true
    }

    @Test
    fun keepsOnlyTheChapterNumber() = runTest {
        queue.edit {
            putString("7", "3:12.5")
            putString("8", "4:2")
        }
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        queue.getFloat("7", 0f) shouldBe 12.5f
        queue.getFloat("8", 0f) shouldBe 2f
        queue.all.size shouldBe 2
    }
}
