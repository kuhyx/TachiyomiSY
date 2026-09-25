package eu.kanade.domain.source.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.getExtensionPackage
import eu.kanade.tachiyomi.extension.getExtensionPackageAsFlow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetIncognitoStateTest {

    private val basePreferences = BasePreferences(mockk(relaxed = true), FlowPreferenceStore())
    private val sourcePreferences = SourcePreferences(FlowPreferenceStore())
    private val extensionManager = mockk<ExtensionManager>()
    private val interactor = GetIncognitoState(basePreferences, sourcePreferences, extensionManager)

    @BeforeEach
    fun setUp() {
        mockkStatic("eu.kanade.tachiyomi.extension.ExtensionManagerRegistryKt")
        every { extensionManager.getExtensionPackage(1) } returns "pkg.one"
        every { extensionManager.getExtensionPackage(2) } returns null
        every { extensionManager.getExtensionPackageAsFlow(1) } returns flowOf("pkg.one")
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun globalIncognitoWins() = runTest {
        basePreferences.incognitoMode.set(true)
        interactor.await(null) shouldBe true
        interactor.await(1) shouldBe true
        interactor.subscribe(null).first() shouldBe true
        interactor.subscribe(1).first() shouldBe true
    }

    @Test
    fun perExtensionIncognito() = runTest {
        interactor.await(null) shouldBe false
        interactor.await(2) shouldBe false
        interactor.await(1) shouldBe false
        interactor.subscribe(1).first() shouldBe false
        sourcePreferences.incognitoExtensions.set(setOf("pkg.one"))
        interactor.await(1) shouldBe true
        interactor.subscribe(1).first() shouldBe true
        interactor.subscribe(null).first() shouldBe false
    }
}
