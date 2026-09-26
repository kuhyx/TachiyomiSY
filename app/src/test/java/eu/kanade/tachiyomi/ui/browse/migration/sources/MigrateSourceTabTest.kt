package eu.kanade.tachiyomi.ui.browse.migration.sources

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.domain.source.interactor.GetSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.getAppIconForSource
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.TabHost
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrateMangaScreen
import eu.kanade.tachiyomi.ui.browse.source.source
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.manga.manga
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.feature.migration.config.MigrationConfigScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.i18n.MR

private const val REGISTRY = "eu.kanade.tachiyomi.extension.ExtensionManagerRegistryKt"

@RunWith(RobolectricTestRunner::class)
internal class MigrateSourceTabTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()
    private val sorting = mockk<SetMigrateSorting>(relaxed = true)
    private val favorites = mockk<GetFavorites>()
    private val extensions = mockk<ExtensionManager>()

    @Before
    fun setUp() {
        mockkStatic(REGISTRY)
        every { extensions.getAppIconForSource(any()) } returns null
        coEvery { favorites.await() } returns listOf(manga().copy(id = 4L, source = 1L), manga().copy(id = 5L))
        koin.start(
            module {
                single<GetSourcesWithFavoriteCount> {
                    mockk { every { subscribe() } returns MutableStateFlow(listOf(source(1L, name = "Alpha") to 2L)) }
                }
                single { sorting }
                single { favorites }
                single { extensions }
            },
        )
    }

    @After
    fun tearDown() {
        koin.stop()
        unmockkStatic(REGISTRY)
    }

    private fun host() = TabHost { migrateSourceTab() }.also { host ->
        host.show(compose)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("Alpha")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun sourceOpensItsEntries() {
        val host = host()
        host.content.titleRes shouldBe MR.strings.label_migration
        compose.onNodeWithText("Alpha").performClick()
        compose.waitForIdle()
        (host.navigator.lastItem is MigrateMangaScreen) shouldBe true
    }

    @Test
    fun allMigratesEveryEntry() {
        val host = host()
        compose.onNodeWithText("All").performClick()
        // The push arrives through withUIContext, a post to the paused main looper that eventually idles.
        eventually { host.navigator.lastItem is MigrationConfigScreen }
    }

    @Test
    fun sortingToggles() {
        host()
        compose.onNodeWithContentDescription("Alphabetically").performClick()
        verify { sorting.await(SetMigrateSorting.Mode.TOTAL, any()) }
        compose.onNodeWithContentDescription("Ascending").performClick()
        verify { sorting.await(any(), SetMigrateSorting.Direction.DESCENDING) }
    }

    @Test
    fun guideOpensInTheBrowser() {
        val host = host()
        host.action("Source migration guide")
        compose.waitForIdle()
    }
}
