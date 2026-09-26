package eu.kanade.tachiyomi.ui.browse.migration.advanced.design

import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.getAppIconForSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.manga.merged.themedActivity
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import tachiyomi.domain.source.service.SourceManager

private const val REGISTRY = "eu.kanade.tachiyomi.extension.ExtensionManagerRegistryKt"

@RunWith(RobolectricTestRunner::class)
internal class MigrationSourceAdapterTest {
    private val koin = BrowseKoin()
    private val icon = ColorDrawable()
    private val extensions = mockk<ExtensionManager>()
    private val sourceManager = mockk<SourceManager>()

    private fun http(sourceId: Long, sourceName: String): HttpSource = mockk(relaxed = true) {
        every { id } returns sourceId
        every { name } returns sourceName
    }

    @Before
    fun setUp() {
        mockkStatic(REGISTRY)
        every { extensions.getAppIconForSource(1L) } returns icon
        every { extensions.getAppIconForSource(2L) } returns null
        koin.start(
            module {
                single { extensions }
                single { sourceManager }
            },
        )
    }

    @After
    fun tearDown() {
        koin.stop()
        unmockkStatic(REGISTRY)
    }

    private fun bind(vararg items: MigrationSourceItem): List<MigrationSourceHolder> {
        val context = themedActivity()
        val adapter = MigrationSourceAdapter { _, _ -> true }
        val recycler = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
        // The holder loads the icon through View.post, which only runs once the row is attached to a window.
        context.setContentView(recycler)
        adapter.updateDataSet(items.toList())
        recycler.measure(1080, 1920)
        recycler.layout(0, 0, 1080, 1920)
        ShadowLooper.idleMainLooper()
        return adapter.allBoundViewHolders.filterIsInstance<MigrationSourceHolder>()
    }

    @Test
    fun enabledSourcesAreBright() {
        koin.sourcePreferences.enabledLanguages.set(setOf("en"))
        val holder = bind(MigrationSourceItem(http(1L, "one"), sourceEnabled = true)).single()
        holder.binding.title.text.toString() shouldBe "One"
        holder.binding.title.alpha shouldBe 1f
        holder.binding.image.drawable shouldBe icon
        (holder.binding.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG) shouldBe 0
    }

    @Test
    fun disabledSourcesAreStruck() {
        koin.sourcePreferences.enabledLanguages.set(setOf("en", "fr"))
        val source = http(2L, "two")
        val holder = bind(MigrationSourceItem(source, sourceEnabled = false)).single()
        holder.binding.title.text.toString() shouldBe source.toString()
        holder.binding.title.alpha shouldBe 0.3f
        (holder.binding.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG) shouldBe Paint.STRIKE_THRU_TEXT_FLAG
    }

    @Test
    fun itemsCompareBySource() {
        val item = MigrationSourceItem(http(1L, "one"), sourceEnabled = true)
        (item == MigrationSourceItem(http(1L, "other"), sourceEnabled = false)) shouldBe true
        item.equals(item) shouldBe true
        item.equals("x") shouldBe false
        item.hashCode() shouldBe 1L.hashCode()
        item.isDraggable shouldBe true
        item.layoutRes shouldBe R.layout.migration_source_item
    }

    @Test
    fun parcelsRoundTrip() {
        val source = http(1L, "one")
        every { sourceManager.get(1L) } returns source
        every { sourceManager.get(2L) } returns null
        val parcel = MigrationSourceItem(source, sourceEnabled = false).asParcelable()
        parcel shouldBe MigrationSourceItem.MigrationSource(1L, false)
        MigrationSourceItem.fromParcelable(sourceManager, parcel)?.sourceEnabled shouldBe false
        MigrationSourceItem.fromParcelable(sourceManager, parcel.copy(sourceId = 2L)).shouldBeNull()
    }
}
