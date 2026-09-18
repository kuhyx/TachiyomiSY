package tachiyomi.domain.storage.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.storage.FolderProvider

internal class StoragePreferencesTest {

    private val folderProvider: FolderProvider = mockk {
        every { path() } returns "/storage/emulated/0/TachiyomiSY"
    }
    private val preferences = StoragePreferences(folderProvider, InMemoryPreferenceStore())

    @Test
    fun defaultsToProvidedFolder() {
        preferences.baseStorageDirectory.get() shouldBe "/storage/emulated/0/TachiyomiSY"
    }

    @Test
    fun keyIsAppState() {
        Preference.isAppState(preferences.baseStorageDirectory.key()) shouldBe true
        preferences.baseStorageDirectory.key() shouldBe Preference.appStateKey("storage_dir")
    }

    @Test
    fun holdsChosenFolder() {
        preferences.baseStorageDirectory.set("content://tree/primary")

        preferences.baseStorageDirectory.get() shouldBe "content://tree/primary"
    }
}
