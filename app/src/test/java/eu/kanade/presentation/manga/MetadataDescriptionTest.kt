package eu.kanade.presentation.manga

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.Lanraragi
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.source.online.all.NHentai
import eu.kanade.tachiyomi.source.online.english.EightMuses
import eu.kanade.tachiyomi.source.online.english.HBrowse
import eu.kanade.tachiyomi.source.online.english.Pururin
import eu.kanade.tachiyomi.source.online.english.Tsumino
import exh.metadata.metadata.EHentaiSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class MetadataDescriptionTest {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val host = MangaScreenHost(compose)

    @Before
    fun setUp() = host.start()

    @After
    fun tearDown() = host.stop()

    private fun <T : Source> source(type: KClass<T>, sourceId: Long): Source = mockkClass(type, relaxed = true) {
        every { id } returns sourceId
        every { name } returns "Meta"
        every { lang } returns "en"
    }

    @Test
    fun everyMetadataSourceHasAView() {
        val types = listOf(
            EHentai::class,
            MangaDex::class,
            NHentai::class,
            EightMuses::class,
            HBrowse::class,
            Pururin::class,
            Tsumino::class,
            Lanraragi::class,
        )
        val sources = types.mapIndexed { index, type -> source(type, index + 100L) } + plainSource()
        val found = mutableListOf<Boolean>()
        compose.setContent {
            MaterialTheme {
                Column {
                    sources.forEach { source ->
                        val description = metadataDescription(source)
                        found += description != null
                        description?.invoke(screenState(source = source), {}, {})
                    }
                }
            }
        }
        compose.waitForIdle()
        found.take(sources.size) shouldContainExactly List(types.size) { true } + false
    }

    @Test
    fun galleryMetaSearchesAndOpens() {
        val meta = EHentaiSearchMetadata().apply { uploader = "someone" }
        host.show(screenState(source = source(EHentai::class, 200L), meta = meta))
        compose.runOnUiThread {
            compose.activity.findViewById<View>(R.id.uploader).performClick()
            compose.activity.findViewById<View>(R.id.more_info).performClick()
        }
        compose.waitForIdle()
        host.events shouldContainExactly listOf("search uploader:\"someone\" false", "metadata")
        host.tablet = true
        compose.waitForIdle()
        compose.activity.findViewById<View>(R.id.uploader).visibility shouldBe View.VISIBLE
    }
}
