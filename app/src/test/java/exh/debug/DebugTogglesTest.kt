package exh.debug

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.PreferenceStore

internal class DebugTogglesTest {
    private val stored = InMemoryPreference(
        key = "eh_debug_toggle_enable_debug_overlay",
        data = false,
        defaultValue = true,
    )
    private val store = InMemoryPreferenceStore(sequenceOf(stored))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @BeforeEach
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single<PreferenceStore> { store } }) }
    }

    @AfterEach
    fun tearDown() {
        scope.cancel()
        stopKoin()
    }

    @Test
    fun defaultsPerToggle() {
        DebugToggles.entries.map { it.default } shouldContainExactly listOf(true, true, true, true, false)
        DebugToggles.entries.map { it.name } shouldContainExactly listOf(
            "ENABLE_EXH_ROOT_REDIRECT",
            "ENABLE_DEBUG_OVERLAY",
            "PULL_TO_ROOT_WHEN_LOADING_EXH_MANGA_DETAILS",
            "RESTRICT_EXH_GALLERY_UPDATE_CHECK_FREQUENCY",
            "INCLUDE_ONLY_ROOT_WHEN_LOADING_EXH_VERSIONS",
        )
    }

    @Test
    fun storedValueWinsOverTheDefault() {
        DebugToggles.ENABLE_DEBUG_OVERLAY.enabled.shouldBeFalse()
        DebugToggles.ENABLE_EXH_ROOT_REDIRECT.enabled.shouldBeTrue()
        DebugToggles.INCLUDE_ONLY_ROOT_WHEN_LOADING_EXH_VERSIONS.enabled.shouldBeFalse()
    }

    @Test
    fun settingWritesThroughTheStore() {
        DebugToggles.ENABLE_EXH_ROOT_REDIRECT.enabled = false
        // InMemoryPreferenceStore hands out a fresh preference per lookup, so the write is not read back.
        DebugToggles.ENABLE_EXH_ROOT_REDIRECT.enabled.shouldBeTrue()
        DebugToggles.preferenceStore shouldBe store
    }

    @Test
    fun asPrefFollowsTheStoredValue() {
        DebugToggles.ENABLE_DEBUG_OVERLAY.asPref(scope).value.shouldBeFalse()
        DebugToggles.ENABLE_EXH_ROOT_REDIRECT.asPref(scope).value.shouldBeTrue()
    }
}
