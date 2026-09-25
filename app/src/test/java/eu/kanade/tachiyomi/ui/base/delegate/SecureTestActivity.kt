package eu.kanade.tachiyomi.ui.base.delegate

import eu.kanade.tachiyomi.ui.base.activity.BaseActivity

/** A [BaseActivity] that opts in to the secure delegate, as the app's own activities do. */
internal class SecureTestActivity : BaseActivity() {
    init {
        registerSecureActivity(this)
    }
}
