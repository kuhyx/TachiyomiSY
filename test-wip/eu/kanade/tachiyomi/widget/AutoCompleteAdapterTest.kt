package eu.kanade.tachiyomi.widget

import android.R
import android.content.Context
import android.database.DataSetObserver
import android.widget.Filter
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AutoCompleteAdapterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val adapter = AutoCompleteAdapter(
        context = context,
        resource = R.layout.simple_list_item_1,
        objects = listOf("Action", "Romance", "Slice of life"),
        validPrefixes = listOf("-", "female:"),
    )

    private fun perform(constraint: CharSequence?): Filter.FilterResults {
        val method = adapter.filter.javaClass.getDeclaredMethod("performFiltering", CharSequence::class.java)
        method.isAccessible = true
        return method.invoke(adapter.filter, constraint) as Filter.FilterResults
    }

    private fun publish(results: Filter.FilterResults) {
        val method = adapter.filter.javaClass.getDeclaredMethod(
            "publishResults",
            CharSequence::class.java,
            Filter.FilterResults::class.java,
        )
        method.isAccessible = true
        method.invoke(adapter.filter, null, results)
    }

    @Test
    fun itemsComeFromObjects() {
        adapter.count shouldBe 3
        adapter.getItem(1) shouldBe "Romance"
        adapter.filter shouldBeSameInstanceAs adapter.filter
    }

    @Test
    fun blankConstraintKeepsAll() {
        perform(null).count shouldBe 3
        perform(" ").values shouldBe listOf("Action", "Romance", "Slice of life")
    }

    @Test
    fun plainConstraintFilters() {
        val results = perform("ro")
        results.values shouldBe listOf("Romance")
        results.count shouldBe 1
    }

    @Test
    fun prefixIsKeptOnResults() {
        val results = perform("-LIFE")
        results.values shouldBe listOf("-Slice of life")
        results.count shouldBe 1
    }

    @Test
    fun publishNotifiesObservers() {
        var changed = 0
        var invalidated = 0
        adapter.registerDataSetObserver(
            object : DataSetObserver() {
                override fun onChanged() {
                    changed++
                }

                override fun onInvalidated() {
                    invalidated++
                }
            },
        )
        publish(perform("act"))
        adapter.objects shouldBe listOf("Action")
        changed shouldBe 1
        publish(Filter.FilterResults())
        adapter.objects shouldBe emptyList()
        invalidated shouldBe 1
    }

    @Test
    fun nonListValuesPublishEmpty() {
        val results = Filter.FilterResults()
        results.values = "not a list"
        results.count = 1
        publish(results)
        adapter.objects shouldBe emptyList()
    }
}
