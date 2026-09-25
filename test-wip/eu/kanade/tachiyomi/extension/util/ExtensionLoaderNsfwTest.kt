package eu.kanade.tachiyomi.extension.util

import eu.kanade.domain.source.service.SourcePreferences
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

/**
 * [ExtensionLoader.loadNsfwSource] is a `by lazy` on a global object, so it can only be resolved
 * once per JVM; every other test in this package stubs it instead of reading it.
 */
@RunWith(RobolectricTestRunner::class)
internal class ExtensionLoaderNsfwTest {
    @After
    fun tearDown() = stopKoin()

    @Test
    fun theFlagComesFromThePreference() {
        stopKoin()
        val store = InMemoryPreferenceStore()
        startKoin { modules(module { single { SourcePreferences(store) } }) }
        ExtensionLoader.loadNsfwSource shouldBe SourcePreferences(store).showNsfwSource.get()
    }
}
