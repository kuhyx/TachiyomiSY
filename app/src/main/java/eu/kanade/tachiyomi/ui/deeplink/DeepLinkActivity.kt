package eu.kanade.tachiyomi.ui.deeplink

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import eu.kanade.tachiyomi.ui.main.MainActivity

internal class DeepLinkActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A fresh intent rather than the received one relaunched: an outside caller must not be
        // able to smuggle flags or a target component through this exported activity.
        val forwarded = Intent(applicationContext, MainActivity::class.java).apply {
            action = intent.action
            setDataAndType(intent.data, intent.type)
            FORWARDED_EXTRAS.forEach { key -> intent.getStringExtra(key)?.let { putExtra(key, it) } }
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(forwarded)
        finish()
    }

    private companion object {
        /** The string extras MainActivity reads off a search, share or SY search intent. */
        val FORWARDED_EXTRAS = listOf(
            SearchManager.QUERY,
            Intent.EXTRA_TEXT,
            MainActivity.INTENT_SEARCH_QUERY,
            MainActivity.INTENT_SEARCH_FILTER,
        )
    }
}
