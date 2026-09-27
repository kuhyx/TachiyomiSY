package eu.kanade.tachiyomi.ui.browse.extension.details

import android.widget.EditText
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.FragmentActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
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

/** A detached fragment has no context; an empty dialog title and a source's own bind listener are kept. */
@RunWith(RobolectricTestRunner::class)
internal class SourcePreferencesFragmentTest {
    private var controller: ActivityController<FragmentActivity>? = null
    private var bound = 0

    @After
    fun tearDown() {
        // A live activity would keep its loading spinner animating into later classes.
        controller?.pause()?.stop()?.destroy()
        stopKoin()
    }

    private fun source(): CatalogueSource {
        val source = mockk<CatalogueSource>(relaxed = true, moreInterfaces = arrayOf(ConfigurableSource::class))
        every { source.id } returns 1L
        every { (source as ConfigurableSource).setupPreferenceScreen(any()) } answers {
            val screen = firstArg<PreferenceScreen>()
            screen.addPreference(
                ListPreference(screen.context).apply {
                    title = "Pick"
                    dialogTitle = ""
                },
            )
            screen.addPreference(
                EditTextPreference(screen.context).apply {
                    title = "Name"
                    setOnBindEditTextListener { bound++ }
                },
            )
        }
        return source
    }

    private fun show(): SourcePreferencesFragment {
        val sources = mockk<SourceManager> {
            every { isInitialized } returns MutableStateFlow(true)
            every { getOrStub(1L) } returns source()
        }
        val built = Robolectric.buildActivity(FragmentActivity::class.java)
        controller = built
        val activity = built.get()
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
        built.setup()
        activity.setContent {
            MaterialTheme {
                val screen = SourcePreferencesScreen(1L)
                Navigator(listOf(BlankScreen(), screen)) { screen.Content() }
            }
        }
        repeat(20) { ShadowLooper.idleMainLooper() }
        return activity.supportFragmentManager.fragments.filterIsInstance<SourcePreferencesFragment>().single()
    }

    @Test
    fun detachedHasNoContext() {
        SourcePreferencesFragment().context.shouldBeNull()
    }

    @Test
    fun titlesAndListenersKept() {
        val fragment = show()
        eventually { fragment.preferenceScreen != null }
        val screen = fragment.preferenceScreen
        (screen.getPreference(0) as ListPreference).dialogTitle shouldBe "Pick"
        val name = screen.getPreference(1) as EditTextPreference
        // The listener getter is package-private in androidx.preference.
        val getter = EditTextPreference::class.java.getDeclaredMethod("getOnBindEditTextListener")
        getter.isAccessible = true
        (getter.invoke(name) as EditTextPreference.OnBindEditTextListener).onBindEditText(EditText(fragment.context))
        bound shouldBe 1
    }
}
