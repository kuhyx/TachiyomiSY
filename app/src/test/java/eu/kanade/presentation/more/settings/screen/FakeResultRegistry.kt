package eu.kanade.presentation.more.settings.screen

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider

/**
 * An [ActivityResultRegistry] that answers every launch at once with what [answer] builds from the
 * contract, so a screen's result callback runs without a real activity. [failure], when set, is thrown
 * from the launch instead.
 */
internal class FakeResultRegistry : ActivityResultRegistry() {
    val launched: MutableList<Any?> = mutableListOf()
    var answer: (ActivityResultContract<*, *>) -> Any? = { null }
    var failure: RuntimeException? = null

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        failure?.let { throw it }
        val context = ApplicationProvider.getApplicationContext<Context>()
        launched += contract.createIntent(context, input)
        dispatchResult(requestCode, answer(contract))
    }

    fun owner(): ActivityResultRegistryOwner {
        val registry = this
        return object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
    }
}
