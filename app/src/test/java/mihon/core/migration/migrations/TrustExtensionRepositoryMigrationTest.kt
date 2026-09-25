package mihon.core.migration.migrations

import eu.kanade.domain.source.service.SourcePreferences
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class TrustExtensionRepositoryMigrationTest {

    private val migration = TrustExtensionRepositoryMigration()
    private val preferences = SourcePreferences(InMemoryPreferenceStore())
    private val repository = mockk<ExtensionStoreRepository>()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 67f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { preferences } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { repository } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun movesEachRepoAndClearsPref() = runTest {
        coEvery { repository.insertFromPreference(any(), any()) } returns Unit
        preferences.extensionRepos.set(
            setOf("https://a.example/index.min.json", "https://b.example/index.json", "https://c.example"),
        )
        startMigrationKoin {
            single { preferences }
            single { repository }
        }
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 1) { repository.insertFromPreference("https://a.example/repo.json", "Repo #1") }
        coVerify(exactly = 1) { repository.insertFromPreference("https://b.example/repo.json", "Repo #2") }
        coVerify(exactly = 1) { repository.insertFromPreference("https://c.example/repo.json", "Repo #3") }
        preferences.extensionRepos.isSet() shouldBe false
    }

    @Test
    fun carriesOnWhenOneRepoFails() = runTest {
        coEvery {
            repository.insertFromPreference("https://bad.example/repo.json", any())
        } throws IllegalStateException()
        coEvery { repository.insertFromPreference("https://good.example/repo.json", any()) } returns Unit
        preferences.extensionRepos.set(setOf("https://bad.example", "https://good.example"))
        startMigrationKoin {
            single { preferences }
            single { repository }
        }
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 1) { repository.insertFromPreference("https://good.example/repo.json", "Repo #2") }
        preferences.extensionRepos.isSet() shouldBe false
    }
}
