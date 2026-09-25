package exh.recs.batch

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.RecommendationSearchBottomSheetBinding
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecSearchBottomSheetDialogTest {
    private val harness = SourceTestHarness()
    private lateinit var preferences: SourcePreferences
    private var searched = 0

    @Before
    fun setUp() {
        harness.install()
        preferences = SourcePreferences(harness.store)
        harness.serve(preferences)
    }

    @After
    fun tearDown() = harness.uninstall()

    // The layout reads Material theme attributes, so the inflater needs the app's own theme.
    private fun binding(): RecommendationSearchBottomSheetBinding {
        val themed = ContextThemeWrapper(harness.application, R.style.Theme_Tachiyomi)
        return RecommendationSearchBottomSheetBinding.inflate(LayoutInflater.from(themed))
    }

    private fun state() = RecommendationSearchBottomSheetDialogState { searched++ }

    @Test
    fun flagsSeedTheCheckboxes() {
        preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_TRACKERS)
        val binding = binding()
        state().initPreferences(binding)
        binding.recSources.isChecked shouldBe false
        binding.recTrackers.isChecked shouldBe true
        binding.recHideLibraryEntries.isChecked shouldBe false
        binding.recSearchBtn.isEnabled shouldBe true
    }

    @Test
    fun togglingWritesFlagsBack() {
        preferences.recommendationSearchFlags.set(0)
        val binding = binding()
        state().initPreferences(binding)
        binding.recSearchBtn.isEnabled shouldBe false
        binding.recSources.isChecked = true
        preferences.recommendationSearchFlags.get() shouldBe SearchFlags.INCLUDE_SOURCES
        binding.recHideLibraryEntries.isChecked = true
        preferences.recommendationSearchFlags.get() shouldBe
            (SearchFlags.INCLUDE_SOURCES or SearchFlags.HIDE_LIBRARY_RESULTS)
        binding.recTrackers.isChecked = true
        preferences.recommendationSearchFlags.get() shouldBe
            (SearchFlags.INCLUDE_SOURCES or SearchFlags.INCLUDE_TRACKERS or SearchFlags.HIDE_LIBRARY_RESULTS)
        binding.recSources.isChecked = false
        binding.recSearchBtn.isEnabled shouldBe true
    }

    @Test
    fun searchButtonReportsBack() {
        preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_SOURCES)
        val binding = binding()
        state().initPreferences(binding)
        binding.recSearchBtn.performClick()
        searched shouldBe 1
    }
}
