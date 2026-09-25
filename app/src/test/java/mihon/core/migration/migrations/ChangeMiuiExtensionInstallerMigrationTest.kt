package mihon.core.migration.migrations

import android.content.Context
import android.content.pm.PackageManager
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.base.BasePreferences.ExtensionInstaller
import eu.kanade.tachiyomi.util.system.DeviceUtil
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class ChangeMiuiExtensionInstallerMigrationTest {

    private val migration = ChangeMiuiExtensionInstallerMigration()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
        unmockkAll()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 27f
    }

    @Test
    fun failsWithoutPreferences() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun leavesNonMiuiDevices() = runTest {
        val preferences = prefsWithoutMiuiInstaller()
        mockkObject(DeviceUtil)
        every { DeviceUtil.isMiui } returns false
        startMigrationKoin { single { preferences } }
        migration(migrationContext()) shouldBe true
        preferences.extensionInstaller.get() shouldBe ExtensionInstaller.PACKAGEINSTALLER
    }

    @Test
    fun leavesOtherInstallersOnMiui() = runTest {
        val preferences = prefsWithoutMiuiInstaller()
        preferences.extensionInstaller.set(ExtensionInstaller.PRIVATE)
        mockkObject(DeviceUtil)
        every { DeviceUtil.isMiui } returns true
        startMigrationKoin { single { preferences } }
        migration(migrationContext()) shouldBe true
        preferences.extensionInstaller.get() shouldBe ExtensionInstaller.PRIVATE
    }

    @Test
    fun switchesMiuiToLegacy() = runTest {
        val preferences = prefsWithoutMiuiInstaller()
        preferences.extensionInstaller.set(ExtensionInstaller.PACKAGEINSTALLER)
        mockkObject(DeviceUtil)
        every { DeviceUtil.isMiui } returns true
        startMigrationKoin { single { preferences } }
        migration(migrationContext()) shouldBe true
        preferences.extensionInstaller.get() shouldBe ExtensionInstaller.LEGACY
    }

    // A context whose package manager knows no MIUI package installer, so the preference keeps its value.
    private fun prefsWithoutMiuiInstaller(): BasePreferences {
        val packageManager = mockk<PackageManager> {
            every { getApplicationInfo(any<String>(), any<Int>()) } throws PackageManager.NameNotFoundException()
        }
        val context = mockk<Context>()
        every { context.packageManager } returns packageManager
        return BasePreferences(context, InMemoryPreferenceStore())
    }
}
