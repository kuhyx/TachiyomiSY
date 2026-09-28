package eu.kanade.tachiyomi.ui.main

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import eu.kanade.presentation.more.settings.screen.about.Changelog
import eu.kanade.presentation.more.settings.screen.about.toDisplayChangelog
import eu.kanade.tachiyomi.util.system.isDebuggable
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import exh.SY_DEBUG_VERSION
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import mihon.core.migration.Migrator
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What only a non-debuggable build does at launch: the changelog after a migration, and preview analytics. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MainNonDebuggableTest {
    private val rig = MainActivityRig()

    @Before
    fun setUp() {
        rig.start()
        loadKoinModules(module { single { XML.v1 { policy { ignoreUnknownChildren() } } } })
        mockkStatic("eu.kanade.tachiyomi.util.system.BuildConfigKt")
        every { isDebuggable } returns false
        mockkObject(Migrator)
        every { Migrator.awaitAndRelease() } returns true
    }

    // The rig's stop unmocks everything, Migrator and the build-type getters included.
    @After
    fun tearDown() = rig.stop()

    @Test
    fun migrationShowsTheChangelog() {
        mockkStatic("eu.kanade.presentation.more.settings.screen.about.WhatsNewDialogKt")
        var decoded = false
        every { any<Changelog>().toDisplayChangelog() } answers {
            decoded = true
            callOriginal()
        }
        rig.launch()
        rig.until { decoded }
    }

    @Test
    fun previewReportsItsVersion() {
        every { isPreviewBuildType } returns true
        val analytics = mockk<FirebaseAnalytics>(relaxed = true)
        mockkStatic("com.google.firebase.analytics.AnalyticsKt")
        every { Firebase.analytics } returns analytics
        rig.launch()
        verify { analytics.setUserProperty("preview_version", SY_DEBUG_VERSION) }
    }
}
