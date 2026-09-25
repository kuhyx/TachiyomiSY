package eu.kanade.domain.extension.interactor

import android.content.pm.PackageInfo
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.service.SourcePreferences
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TrustExtensionTest {

    private val repository = mockk<ExtensionStoreRepository>()
    private val preferences = SourcePreferences(FlowPreferenceStore())
    private val interactor = TrustExtension(repository, preferences)
    private val packageInfo = PackageInfo().apply {
        packageName = "pkg.one"
        longVersionCode = 7
    }

    @Test
    fun trustedByStoreKey() = runTest {
        coEvery { repository.getAll() } returns listOf(testStore)
        interactor.isTrusted(packageInfo, listOf("other", "key")) shouldBe true
        interactor.isTrusted(packageInfo, listOf("other")) shouldBe false
    }

    @Test
    fun trustedByUserDecision() = runTest {
        coEvery { repository.getAll() } returns emptyList()
        interactor.isTrusted(packageInfo, listOf("hash")) shouldBe false
        interactor.trust("pkg.one", 6, "old")
        interactor.trust("pkg.two", 1, "two")
        interactor.trust("pkg.one", 7, "hash")
        preferences.trustedExtensions.get() shouldBe setOf("pkg.two:1:two", "pkg.one:7:hash")
        interactor.isTrusted(packageInfo, listOf("hash")) shouldBe true
        interactor.revokeAll()
        preferences.trustedExtensions.isSet() shouldBe false
        interactor.isTrusted(packageInfo, listOf("hash")) shouldBe false
    }
}
