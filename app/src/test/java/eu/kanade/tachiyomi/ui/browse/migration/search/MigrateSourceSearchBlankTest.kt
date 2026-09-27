package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseScreenRig
import eu.kanade.tachiyomi.ui.browse.source.browse.listed
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Searching a source for the migration target without a query lists the source as it is. */
@RunWith(RobolectricTestRunner::class)
internal class MigrateSourceSearchBlankTest {
    @get:Rule
    val compose = createComposeRule()

    private val rig = BrowseScreenRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun noQueryListsTheSource() {
        rig.show(MigrateSourceSearchScreen(listed(7L), 1L, null))
        rig.await("Manga 1")
    }
}
