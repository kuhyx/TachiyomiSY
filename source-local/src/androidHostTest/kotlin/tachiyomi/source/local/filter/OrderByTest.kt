package tachiyomi.source.local.filter

import android.content.Context
import eu.kanade.tachiyomi.source.model.Filter
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class OrderByTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun popularSortsByTitleAscending() {
        val filter = OrderBy.Popular(context)
        filter.name shouldBe context.stringResource(MR.strings.local_filter_order_by)
        filter.values.toList() shouldBe listOf(
            context.stringResource(MR.strings.title),
            context.stringResource(MR.strings.date),
        )
        filter.state shouldBe Filter.Sort.Selection(0, true)
    }

    @Test
    fun latestSortsByDateDescending() {
        val filter = OrderBy.Latest(context)
        filter.name shouldBe context.stringResource(MR.strings.local_filter_order_by)
        filter.values.size shouldBe 2
        filter.state shouldBe Filter.Sort.Selection(1, false)
    }
}
