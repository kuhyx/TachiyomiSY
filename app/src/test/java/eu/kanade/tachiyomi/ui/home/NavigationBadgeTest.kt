package eu.kanade.tachiyomi.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences

/** A store whose ints and booleans report their current value once and complete. */
internal class OneShotCountsStore(
    private val inner: PreferenceStore = MapPreferenceStore(),
) : PreferenceStore by inner {
    override fun getInt(key: String, defaultValue: Int): Preference<Int> = once(inner.getInt(key, defaultValue))

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        once(inner.getBoolean(key, defaultValue))

    private fun <T> once(preference: Preference<T>): Preference<T> = object : Preference<T> by preference {
        override fun changes(): Flow<T> = flowOf(preference.get())
    }
}

/** The update badges read their counts to the end when the preferences stop changing. */
@RunWith(RobolectricTestRunner::class)
internal class NavigationBadgeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val store = OneShotCountsStore()

    @Before
    fun setUp() {
        LibraryPreferences(store).newUpdatesCount.set(3)
        SourcePreferences(store).extensionUpdatesCount.set(2)
        stopKoin()
        startKoin {
            modules(
                module {
                    single { LibraryPreferences(store) }
                    single { SourcePreferences(store) }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun badgesShowFinalCounts() {
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
        compose.setContent {
            MaterialTheme {
                TabNavigator(UpdatesTab) {
                    Row {
                        NavigationIconItem(UpdatesTab)
                        NavigationIconItem(BrowseTab)
                    }
                }
            }
        }
        compose.pollLabel("3")
        compose.pollLabel("2")
    }
}
