package mihon.feature.migration.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import mihon.domain.migration.models.MigrationFlag
import mihon.domain.migration.usecases.MigrateMangaUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.manga.model.Manga
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class MigrateMangaDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val preferences = SourcePreferences(InMemoryPreferenceStore())
    private lateinit var coverCache: CoverCache
    private lateinit var downloads: DownloadManager
    private lateinit var migrate: MigrateMangaUseCase
    private val events = mutableListOf<String>()
    private val current = Manga.create().copy(id = 1L, ogTitle = "Current", notes = "some notes")
    private val target = Manga.create().copy(id = 2L, ogTitle = "Target")

    @Before
    fun setUp() {
        coverCache = mockk()
        downloads = mockk()
        migrate = mockk()
        every { coverCache.getCustomCoverFile(any()) } returns File(folder.root, "missing")
        every { downloads.getDownloadCount(any<Manga>()) } returns 0
        coEvery { migrate(any(), any(), any(), any()) } returns Unit
        stopKoin()
        startKoin {
            modules(
                module {
                    single { preferences }
                    single { coverCache }
                    single { downloads }
                    single { migrate }
                },
            )
        }
    }

    @After
    fun tearDown() {
        clearVoyagerScopes()
        stopKoin()
    }

    private fun show(from: Manga = current, withComplete: Boolean = true) {
        val screen = DialogScreen(from, target, events, withComplete)
        compose.setContent { ScreenHost(screen) }
        compose.waitForLabel("Target")
        compose.waitForLabel("Chapters")
    }

    private fun click(label: String) {
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle()
    }

    @Test
    fun everyApplicableFlagIsListed() {
        every { coverCache.getCustomCoverFile(any()) } returns folder.newFile("cover")
        every { downloads.getDownloadCount(any<Manga>()) } returns 3
        show()
        listOf("Categories", "Custom cover", "Notes", "Delete downloaded").forEach { compose.waitForLabel(it) }
    }

    @Test
    fun onlyTheBasicFlagsByDefault() {
        show(from = current.copy(notes = " "))
        compose.hasLabel("Categories") shouldBe true
        compose.hasLabel("Custom cover") shouldBe false
        compose.hasLabel("Notes") shouldBe false
        compose.hasLabel("Delete downloaded") shouldBe false
    }

    @Test
    fun migrateKeepsTheChosenFlags() {
        show()
        click("Chapters")
        click("Chapters")
        click("Categories")
        click("Migrate")
        eventually { events.contains("complete") }
        coVerify(timeout = 5_000) { migrate(current, target, true, any()) }
        preferences.migrationFlags.get().contains(MigrationFlag.CATEGORY) shouldBe false
        preferences.migrationFlags.get().contains(MigrationFlag.CHAPTER) shouldBe true
        compose.waitForIdle()
        compose.hasLabel("Copy") shouldBe false
    }

    @Test
    fun copyDismissesByDefault() {
        show(withComplete = false)
        click("Copy")
        eventually { events.contains("dismiss") }
        coVerify(timeout = 5_000) { migrate(current, target, false, any()) }
    }

    @Test
    fun showMangaOpensTheTitle() {
        show()
        click("Show entry")
        events shouldContainExactly listOf("dismiss", "title")
    }

    @Test
    fun aSpinnerShowsWhileMigrating() {
        val gate = CompletableDeferred<Unit>()
        coEvery { migrate(any(), any(), any(), any()) } coAnswers { gate.await() }
        show()
        click("Migrate")
        eventually { !compose.hasLabel("Copy") }
        gate.complete(Unit)
        eventually { events.contains("complete") }
    }
}

/** Hosts the dialog in a screen, as the browse and manga screens do. */
private class DialogScreen(
    private val current: Manga,
    private val target: Manga,
    private val events: MutableList<String>,
    private val withComplete: Boolean,
) : Screen {
    @Composable
    override fun Content() {
        Text(target.title)
        if (withComplete) {
            MigrateMangaDialog(
                current = current,
                target = target,
                onClickTitle = { events += "title" },
                onDismissRequest = { events += "dismiss" },
                onComplete = { events += "complete" },
            )
        } else {
            MigrateMangaDialog(
                current = current,
                target = target,
                onClickTitle = { events += "title" },
                onDismissRequest = { events += "dismiss" },
            )
        }
    }
}
