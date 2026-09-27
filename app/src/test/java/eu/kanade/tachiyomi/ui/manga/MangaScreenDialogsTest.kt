package eu.kanade.tachiyomi.ui.manga

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.MangaWithChapterCount

/** The add-to-library, duplicate, migrate, interval and delete dialogs of the manga screen. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MangaScreenDialogsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = MangaDialogsRig(compose)
    private val other = manga().copy(id = 2L, ogTitle = "Other one")
    private val category = Category(id = 3L, name = "Three", order = 1L, flags = 0L)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() {
        rig.stop()
        unmockkAll()
    }

    @Test
    fun noDialogShowsNothing() {
        rig.show(null)
        compose.onNodeWithText("Three").assertDoesNotExist()
    }

    @Test
    fun categoriesAreApplied() {
        rig.show(MangaScreenModel.Dialog.ChangeCategory(manga(), listOf(CheckboxState.State.None(category))))
        rig.await("Three")
        rig.click("OK")
        coVerify(timeout = 5_000) { rig.harness.parts.setMangaCategories.await(1L, any()) }
    }

    @Test
    fun categoriesCanBeEdited() {
        rig.show(MangaScreenModel.Dialog.ChangeCategory(manga(), listOf(CheckboxState.State.None(category))))
        rig.await("Edit")
        rig.click("Edit")
        rig.navigator.lastItem.shouldBeInstanceOf<CategoryScreen>()
    }

    @Test
    fun duplicateAddedAnyway() {
        rig.show(MangaScreenModel.Dialog.DuplicateManga(manga(), listOf(MangaWithChapterCount(other, 1L))))
        rig.await("Other one")
        rig.click("Add anyway")
        coVerify(timeout = 5_000) { rig.harness.updateManga.awaitUpdateFavorite(1L, any()) }
    }

    @Test
    fun duplicateOpensOrMigrates() {
        rig.show(MangaScreenModel.Dialog.DuplicateManga(manga(), listOf(MangaWithChapterCount(other, 1L))))
        rig.await("Other one")
        compose.onNodeWithText("Other one").performTouchInput { longClick() }
        compose.waitForIdle()
        rig.navigator.lastItem.shouldBeInstanceOf<MangaScreen>()
        rig.click("Other one")
        rig.model.awaitSuccess { it.dialog is MangaScreenModel.Dialog.Migrate }
    }

    @Test
    fun migrateOpensCurrent() {
        rig.show(MangaScreenModel.Dialog.Migrate(target = manga(), current = other))
        rig.await("Show entry")
        rig.click("Show entry")
        rig.navigator.lastItem.shouldBeInstanceOf<MangaScreen>()
    }

    @Test
    fun fetchIntervalIsEditable() {
        rig.harness.libraryPreferences.autoUpdateMangaRestrictions.set(
            setOf(LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD),
        )
        rig.show(MangaScreenModel.Dialog.SetFetchInterval(manga().copy(fetchInterval = 3)))
        rig.await("OK")
        rig.click("OK")
        coVerify(timeout = 5_000) { rig.harness.updateManga.awaitUpdateFetchInterval(any(), any(), any()) }
    }

    @Test
    fun fetchIntervalReadOnly() {
        rig.harness.libraryPreferences.autoUpdateMangaRestrictions.set(emptySet())
        rig.show(MangaScreenModel.Dialog.SetFetchInterval(manga()))
        rig.await("OK")
        rig.click("OK")
        rig.model.isUpdateIntervalEnabled shouldBe false
    }

    @Test
    fun deletingChapters() {
        mockkStatic(DELETION)
        every { rig.harness.downloadManager.deleteChapters(any(), any(), any()) } just runs
        rig.show(MangaScreenModel.Dialog.DeleteChapters(listOf(chapter(1L))))
        rig.await("OK")
        rig.click("OK")
        verify(timeout = 5_000) { rig.harness.downloadManager.deleteChapters(listOf(chapter(1L)), any(), any()) }
    }
}
