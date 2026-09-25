package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.backupExtensionStore
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.Extension_storeQueries

internal class ExtensionStoreRestorerTest {

    private val stores = mockk<Extension_storeQueries>(relaxed = true)
    private val database = mockk<Database> { every { extension_storeQueries } returns stores }

    @Test
    fun filledStoreIsCopied() = runTest {
        ExtensionStoreRestorer(database)(backupExtensionStore())
        coVerify {
            stores.upsert(
                indexUrl = "https://index",
                name = "Store",
                badgeLabel = "badge",
                signingKey = "key",
                contactWebsite = "https://site",
                contactDiscord = "discord",
                isLegacy = false,
                extensionListUrl = "https://list",
            )
        }
    }

    @Test
    fun missingFieldsGetDefaults() = runTest {
        val store = backupExtensionStore(
            badgeLabel = null,
            contactDiscord = null,
            isLegacy = null,
            extensionListUrl = null,
        )
        ExtensionStoreRestorer(database)(store)
        coVerify {
            stores.upsert(
                indexUrl = "https://index",
                name = "Store",
                badgeLabel = "Store",
                signingKey = "key",
                contactWebsite = "https://site",
                contactDiscord = null,
                isLegacy = true,
                extensionListUrl = null,
            )
        }
    }
}
