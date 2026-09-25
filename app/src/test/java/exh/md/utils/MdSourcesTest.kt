package exh.md.utils

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.source.online.all.MangaDexFixture
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class MdSourcesTest {
    private val harness = SourceTestHarness()
    private lateinit var english: MangaDex
    private lateinit var japanese: MangaDex
    private lateinit var preferences: SourcePreferences
    private lateinit var sourceManager: SourceManager

    @Before
    fun setUp() {
        harness.install()
        english = MangaDexFixture(harness, lang = "en").source
        japanese = MangaDexFixture(harness, lang = "ja").source
        preferences = SourcePreferences(harness.store)
        preferences.enabledLanguages.set(setOf("en", "ja"))
        val other = mockk<HttpSource> { every { id } returns 1L }
        sourceManager = mockk { every { getVisibleOnlineSources() } returns listOf(other, english, japanese) }
        harness.serve(preferences)
        harness.serve(sourceManager)
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun enabledDexsAreFiltered() {
        MdUtil.getEnabledMangaDexs(preferences, sourceManager) shouldContainExactly listOf(english, japanese)
        preferences.enabledLanguages.set(setOf("en"))
        MdUtil.getEnabledMangaDexs(preferences, sourceManager) shouldContainExactly listOf(english)
        preferences.disabledSources.set(setOf(english.id.toString()))
        MdUtil.getEnabledMangaDexs(preferences, sourceManager).isEmpty() shouldBe true
        preferences.enabledLanguages.set(setOf("en", "ja"))
        MdUtil.getEnabledMangaDexs(preferences) shouldContainExactly listOf(japanese)
    }

    @Test
    fun preferredDexWins() {
        MdUtil.getEnabledMangaDex(preferences, sourceManager) shouldBe english
        preferences.preferredMangaDexId.set(japanese.id.toString())
        MdUtil.getEnabledMangaDex(preferences, sourceManager) shouldBe japanese
        preferences.preferredMangaDexId.set("999")
        MdUtil.getEnabledMangaDex(preferences, sourceManager) shouldBe english
        preferences.preferredMangaDexId.set("not a number")
        MdUtil.getEnabledMangaDex() shouldBe english
        preferences.preferredMangaDexId.set("0")
        MdUtil.getEnabledMangaDex(sourceManager = sourceManager) shouldBe english
    }

    @Test
    fun noneEnabled() {
        preferences.enabledLanguages.set(emptySet())
        MdUtil.getEnabledMangaDex(preferences, sourceManager).shouldBeNull()
    }
}
