package eu.kanade.tachiyomi.ui.browse.extension.details

import android.widget.EditText
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.FragmentActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import exh.source.EnhancedHttpSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowLooper
import tachiyomi.domain.source.service.SourceManager

/** The screen that hosts a configurable source's own preference fragment. */
@RunWith(RobolectricTestRunner::class)
internal class SourcePreferencesScreenTest {
    private val loaded = MutableStateFlow(true)
    private val sources = mockk<SourceManager> { every { isInitialized } returns loaded }
    private lateinit var activity: FragmentActivity
    private val launched = mutableListOf<ActivityController<FragmentActivity>>()

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).get()
        activity.setTheme(R.style.Theme_Tachiyomi)
        stopKoin()
        startKoin {
            modules(
                module {
                    single { sources }
                    single { activity.application }
                    single { BasePreferences(activity.application, MapPreferenceStore()) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        // A live activity would keep its loading spinner animating into later classes.
        launched.forEach { it.pause().stop().destroy() }
        stopKoin()
    }

    private fun configurable(): Source {
        val source = mockk<CatalogueSource>(relaxed = true, moreInterfaces = arrayOf(ConfigurableSource::class))
        every { source.id } returns 1L
        every { (source as ConfigurableSource).setupPreferenceScreen(any()) } answers {
            val screen = firstArg<PreferenceScreen>()
            screen.addPreference(EditTextPreference(screen.context).apply { title = "Name" })
            screen.addPreference(ListPreference(screen.context).apply { dialogTitle = "Given" })
            screen.addPreference(SwitchPreferenceCompat(screen.context).apply { title = "Flag" })
        }
        return source
    }

    private fun show(source: Source): SourcePreferencesFragment? {
        every { sources.getOrStub(1L) } returns source
        val controller = Robolectric.buildActivity(FragmentActivity::class.java)
        launched += controller
        activity = controller.get()
        activity.setTheme(R.style.Theme_Tachiyomi)
        controller.setup()
        activity.setContent {
            MaterialTheme {
                val screen = SourcePreferencesScreen(1L)
                Navigator(listOf(BlankScreen(), screen)) { screen.Content() }
            }
        }
        repeat(20) { ShadowLooper.idleMainLooper() }
        return activity.supportFragmentManager.fragments.filterIsInstance<SourcePreferencesFragment>().firstOrNull()
    }

    @Test
    fun configurableFillsScreen() {
        val fragment = checkNotNull(show(configurable()))
        eventually { fragment.preferenceScreen != null }
        val screen = fragment.preferenceScreen
        screen.preferenceCount shouldBe 3
        val name = screen.getPreference(0) as EditTextPreference
        name.dialogTitle shouldBe "Name"
        (screen.getPreference(1) as ListPreference).dialogTitle shouldBe "Given"
        name.isIconSpaceReserved shouldBe false
        // The listener getter is package-private in androidx.preference.
        val getter = EditTextPreference::class.java.getDeclaredMethod("getOnBindEditTextListener")
        getter.isAccessible = true
        (getter.invoke(name) as EditTextPreference.OnBindEditTextListener).onBindEditText(EditText(activity))
    }

    @Test
    fun plainSourceHasNoSettings() {
        val fragment = checkNotNull(show(mockk<CatalogueSource>(relaxed = true)))
        eventually { fragment.preferenceScreen != null }
        fragment.preferenceScreen.preferenceCount shouldBe 0
        (fragment.context !== null) shouldBe true
    }

    @Test
    fun enhancedPicksConfigurable() {
        val plain = mockk<CatalogueSource>()
        plain.configurableSide() shouldBe plain
        val wrapper = mockk<HttpSource>()
        val original = mockk<HttpSource>()
        val enhanced = mockk<EnhancedHttpSource> {
            every { enhancedSource } returns mockk<HttpSource>()
            every { originalSource } returns original
        }
        enhanced.configurableSide() shouldBe original
        val configured = mockk<EnhancedHttpSource> {
            every { enhancedSource } returns mockk<HttpSource>(
                moreInterfaces = arrayOf(ConfigurableSource::class),
            )
            every { source() } returns wrapper
        }
        configured.configurableSide() shouldBe wrapper
    }
}
