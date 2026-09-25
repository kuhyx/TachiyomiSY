package eu.kanade.tachiyomi.extension.api

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.anInstalledExtension
import eu.kanade.tachiyomi.source.online.MemoPreferenceStore
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Method

private const val BLACKLISTED_PKG = "eu.kanade.tachiyomi.extension.all.ehentai"

/**
 * `ExtensionApi.isBlacklisted` is private and every call site passes the flag, so its default
 * argument only runs through the synthetic `$default` bridge the compiler emits for it.
 */
@RunWith(RobolectricTestRunner::class)
internal class ExtensionApiBlacklistTest {
    private val store = MemoPreferenceStore()
    private val preferences = SourcePreferences(store)

    @Before
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single { preferences } }) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun bridge(): Method = ExtensionApi::class.java.declaredMethods
        .single { it.name.startsWith("isBlacklisted") && it.parameterCount == 5 }
        .apply { isAccessible = true }

    private fun isBlacklisted(pkgName: String): Boolean = bridge().invoke(
        null,
        ExtensionApi(),
        anInstalledExtension(pkgName = pkgName),
        false,
        1,
        null,
    ) as Boolean

    @Test
    fun theFlagDefaultsToThePreference() {
        preferences.enableSourceBlacklist.set(true)
        isBlacklisted(BLACKLISTED_PKG) shouldBe true
        isBlacklisted("pkg.other") shouldBe false
    }

    @Test
    fun withTheBlacklistOff() {
        preferences.enableSourceBlacklist.set(false)
        isBlacklisted(BLACKLISTED_PKG) shouldBe false
    }
}
