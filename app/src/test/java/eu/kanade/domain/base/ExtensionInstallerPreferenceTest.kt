package eu.kanade.domain.base

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import eu.kanade.domain.base.BasePreferences.ExtensionInstaller
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class ExtensionInstallerPreferenceTest {

    @Test
    fun plainDevicesUsePkgInstaller() {
        val preference = ExtensionInstallerPreference(context(), InMemoryPreferenceStore())
        preference.key() shouldBe "extension_installer"
        preference.defaultValue() shouldBe ExtensionInstaller.PACKAGEINSTALLER
        preference.entries shouldBe ExtensionInstaller.entries.toList()
        preference.get() shouldBe ExtensionInstaller.PACKAGEINSTALLER
        preference.isSet() shouldBe false
    }

    @Test
    fun miuiDevicesDefaultToLegacy() {
        val preference = ExtensionInstallerPreference(context(miui = true), InMemoryPreferenceStore())
        preference.defaultValue() shouldBe ExtensionInstaller.LEGACY
        preference.entries shouldBe ExtensionInstaller.entries.filter { it != ExtensionInstaller.PACKAGEINSTALLER }
        preference.set(ExtensionInstaller.PACKAGEINSTALLER)
        preference.get() shouldBe ExtensionInstaller.LEGACY
    }

    @Test
    fun shizukuNeedsTheApp() {
        val preference = ExtensionInstallerPreference(context(), InMemoryPreferenceStore())
        preference.set(ExtensionInstaller.SHIZUKU)
        preference.get() shouldBe ExtensionInstaller.PACKAGEINSTALLER
        val withShizuku = ExtensionInstallerPreference(context(shizuku = true), InMemoryPreferenceStore())
        withShizuku.set(ExtensionInstaller.SHIZUKU)
        withShizuku.get() shouldBe ExtensionInstaller.SHIZUKU
        withShizuku.isSet() shouldBe true
    }

    @Test
    fun storedValuesAreReCheckedOnRead() = runTest {
        val store = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference(
                    "extension_installer",
                    ExtensionInstaller.SHIZUKU,
                    ExtensionInstaller.PACKAGEINSTALLER,
                ),
            ),
        )
        val preference = ExtensionInstallerPreference(context(), store)
        preference.isSet() shouldBe true
        preference.get() shouldBe ExtensionInstaller.PACKAGEINSTALLER
        preference.set(ExtensionInstaller.PRIVATE)
        preference.get() shouldBe ExtensionInstaller.PRIVATE
        preference.changes().toList() shouldBe emptyList()
        preference.stateIn(this).value shouldBe ExtensionInstaller.PRIVATE
        preference.delete()
        preference.isSet() shouldBe false
    }

    // A context whose package manager knows the MIUI installer and/or Shizuku.
    private fun context(miui: Boolean = false, shizuku: Boolean = false): Context {
        val packageManager = mockk<PackageManager>()
        every { packageManager.getApplicationInfo(any<String>(), any<Int>()) } answers {
            val installed = when (firstArg<String>()) {
                "com.miui.packageinstaller" -> miui
                "moe.shizuku.privileged.api" -> shizuku
                else -> false
            }
            if (installed) ApplicationInfo() else throw PackageManager.NameNotFoundException()
        }
        val context = mockk<Context>()
        every { context.packageManager } returns packageManager
        return context
    }
}
