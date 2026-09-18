package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.tachiyomi.source.online.SourceHarness
import eu.kanade.tachiyomi.source.online.StubSource
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** A [ConfigurableSource] that records the screen it was asked to populate. */
private class ConfigurableStubSource : StubSource(id = 42L), ConfigurableSource {
    var populated: PreferenceScreen? = null

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        populated = screen
    }
}

/** The preference file lookups of [ConfigurableSource], all keyed by the source id. */
internal class ConfigurableSourceTest {
    private val harness = SourceHarness()
    private val source = ConfigurableStubSource()

    @BeforeEach
    fun setUp() = harness.install()

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun preferenceKeyUsesSourceId() {
        source.preferenceKey() shouldBe "source_42"
    }

    @Test
    fun getSourcePreferencesOpensFile() {
        (source.getSourcePreferences() === harness.sharedPreferences) shouldBe true
        verify(exactly = 1) { harness.application.getSharedPreferences("source_42", Context.MODE_PRIVATE) }
    }

    @Test
    fun preferencesExtOpensSameFile() {
        (source.sourcePreferences() === harness.sharedPreferences) shouldBe true
        verify(exactly = 1) { harness.application.getSharedPreferences("source_42", Context.MODE_PRIVATE) }
    }

    @Test
    fun preferencesByKeyOpensFile() {
        (sourcePreferences("custom_key") === harness.sharedPreferences) shouldBe true
        verify(exactly = 1) { harness.application.getSharedPreferences("custom_key", Context.MODE_PRIVATE) }
    }

    @Test
    fun setupScreenIsImplemented() {
        val screen = mockk<PreferenceScreen>()
        source.setupPreferenceScreen(screen)
        (source.populated === screen) shouldBe true
    }
}
