package eu.kanade.domain.extension.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetExtensionsByTypeTest {

    private val preferences = SourcePreferences(FlowPreferenceStore())
    private val extensionManager = mockk<ExtensionManager>()
    private val interactor = GetExtensionsByType(preferences, extensionManager)

    private val installedList = listOf(
        installed("beta", hasUpdate = true),
        installed("Alpha"),
        installed("old", isObsolete = true),
        installed("dup", isRedundant = true),
        installed("adult", isNsfw = true),
    )
    private val availableList = listOf(
        available("zeta", sources = listOf(1L to "en", 2L to "fr")),
        available("Alpha", sources = listOf(3L to "en")),
        available("pending", sources = listOf(4L to "en")),
        available("adult2", sources = listOf(5L to "en"), isNsfw = true),
        available("alone", sources = listOf(6L to "de")),
    )
    private val untrustedList = listOf(untrusted("pending"), untrusted("Another"))

    @Test
    fun splitsAndSortsEveryKind() = runTest {
        every { extensionManager.installedExtensionsFlow } returns MutableStateFlow(installedList)
        every { extensionManager.availableExtensionsFlow } returns MutableStateFlow(availableList)
        every { extensionManager.untrustedExtensionsFlow } returns MutableStateFlow(untrustedList)
        preferences.enabledLanguages.set(setOf("en", "fr"))
        val result = interactor.subscribe().first()
        result.updates.map { it.name } shouldBe listOf("beta")
        result.installed.map { it.name } shouldBe listOf("dup", "old", "adult", "Alpha")
        result.untrusted.map { it.name } shouldBe listOf("Another", "pending")
        result.available.map { it.name } shouldBe listOf("adult2 en", "zeta en", "zeta fr")
        result.available.map { it.pkgName } shouldBe listOf("pkg.adult2-5", "pkg.zeta-1", "pkg.zeta-2")
        result.available.first().sources.single().id shouldBe 5L
    }

    @Test
    fun hidesNsfwWhenDisabled() = runTest {
        every { extensionManager.installedExtensionsFlow } returns MutableStateFlow(installedList)
        every { extensionManager.availableExtensionsFlow } returns MutableStateFlow(availableList)
        every { extensionManager.untrustedExtensionsFlow } returns MutableStateFlow(emptyList<Extension.Untrusted>())
        preferences.showNsfwSource.set(false)
        preferences.enabledLanguages.set(setOf("en"))
        val result = interactor.subscribe().first()
        result.installed.map { it.name } shouldBe listOf("dup", "old", "Alpha")
        result.available.map { it.name } shouldBe listOf("pending en", "zeta en")
    }
}
