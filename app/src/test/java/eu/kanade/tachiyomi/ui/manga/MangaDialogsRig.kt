package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.presentation.more.settings.screen.FakeResultRegistry
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import mihon.domain.migration.usecases.MigrateMangaUseCase
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.FetchInterval

internal typealias MangaCompose = AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

/**
 * [MangaScreenDialogs] for a loaded [MangaHarness] model, showing whichever dialog the test puts in the
 * state; screens it pushes land on a navigator that renders nothing.
 */
internal class MangaDialogsRig(private val compose: MangaCompose) {
    val harness: MangaHarness = MangaHarness()
    val registry: FakeResultRegistry = FakeResultRegistry()
    val imageSaver: ImageSaver = mockk(relaxed = true)
    lateinit var model: MangaScreenModel
    lateinit var navigator: Navigator

    fun start() {
        harness.start(
            module {
                single { imageSaver }
                single<Application> { harness.app }
                single { mockk<MigrateMangaUseCase>(relaxed = true) }
            },
        )
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L))
        harness.scanlators.value = setOf("Group")
        // A mocked UpdateManga has no FetchInterval, which its default arguments read.
        UpdateManga::class.java.getDeclaredField("fetchInterval").also { it.isAccessible = true }
            .set(harness.updateManga, mockk<FetchInterval>(relaxed = true))
        coEvery { harness.parts.getManga.subscribe(any<Long>()) } returns flowOf(manga(favorite = true))
        coEvery { harness.parts.getManga.await(any<Long>()) } returns manga(favorite = true)
    }

    fun stop() = harness.stop()

    fun show(
        dialog: MangaScreenModel.Dialog?,
        edit: (MangaScreenModel.State.Success) -> MangaScreenModel.State.Success = { it },
    ) {
        model = harness.loaded()
        val state = edit(model.awaitSuccess()).copy(dialog = dialog)
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry.owner()) {
                Navigator(BlankScreen()) { current ->
                    navigator = current
                    MangaScreen(1L).MangaScreenDialogs(model, state)
                }
            }
        }
        compose.waitForIdle()
    }

    fun await(label: String) = compose.pollLabel(label)

    fun click(label: String) = compose.clickLabel(label)
}
