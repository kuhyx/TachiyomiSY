package eu.kanade.tachiyomi.ui.deeplink

import android.app.SearchManager
import android.content.Intent
import androidx.core.net.toUri
import eu.kanade.tachiyomi.ui.main.MainActivity
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class DeepLinkActivityTest {
    private fun forward(intent: Intent): Intent {
        val activity = Robolectric.buildActivity(DeepLinkActivity::class.java, intent).create().get()
        activity.isFinishing shouldBe true
        return shadowOf(activity).nextStartedActivity
    }

    @Test
    fun searchIsForwardedToMain() {
        val intent = Intent(Intent.ACTION_SEARCH)
            .putExtra(SearchManager.QUERY, "query")
            .putExtra(MainActivity.INTENT_SEARCH_FILTER, "filter")
            .putExtra("unrelated", "x")
        val forwarded = forward(intent)
        forwarded.component?.className shouldBe MainActivity::class.java.name
        forwarded.action shouldBe Intent.ACTION_SEARCH
        forwarded.getStringExtra(SearchManager.QUERY) shouldBe "query"
        forwarded.getStringExtra(MainActivity.INTENT_SEARCH_FILTER) shouldBe "filter"
        forwarded.getStringExtra("unrelated").shouldBeNull()
        forwarded.flags shouldBe (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun viewKeepsDataAndType() {
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType("https://example.org".toUri(), "text/plain")
        val forwarded = forward(intent)
        forwarded.data.toString() shouldBe "https://example.org"
        forwarded.type shouldBe "text/plain"
        forwarded.getStringExtra(Intent.EXTRA_TEXT).shouldBeNull()
    }
}
