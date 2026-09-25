package exh.util

import android.net.Uri
import eu.kanade.tachiyomi.source.model.Filter
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UriGroupTest {
    private class Param(name: String, private val value: String) : Filter.Text(name), UriFilter {
        override fun addToUri(builder: Uri.Builder) {
            builder.appendQueryParameter(name, value)
        }
    }

    private class Plain : Filter.CheckBox("plain")

    @Test
    fun onlyUriFiltersContribute() {
        val group = UriGroup<Filter<*>>("g", listOf(Param("a", "1"), Plain(), Param("b", "2")))
        val builder = Uri.Builder().scheme("https").authority("example.test")
        group.addToUri(builder)
        builder.build().toString() shouldBe "https://example.test?a=1&b=2"
        group.name shouldBe "g"
    }
}
