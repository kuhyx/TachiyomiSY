package eu.kanade.tachiyomi.ui.base

import cafe.adriel.voyager.core.model.ScreenModelStore
import eu.kanade.tachiyomi.source.online.readMember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/**
 * Voyager keeps each screen model's IO scope in a JVM-wide store keyed by the last screen key. A model
 * built outside a Navigator, or a composition that is never disposed, leaves an entry a later screen can
 * pick up -- already cancelled, so its paging never loads. Cancels and forgets every such entry; touches
 * no Looper, so plain JUnit harnesses can call it too.
 */
internal fun dropScreenModelIoScopes() {
    val dependencies = checkNotNull(ScreenModelStore.readMember(ScreenModelStore::class, "dependencies"))
    val remove = dependencies::class.java.methods.first { it.name == "remove" && it.parameterCount == 1 }
    val entries = (dependencies as Map<*, *>).entries.filter { "IoCoroutineScope" in it.key.toString() }
    entries.forEach { (key, value) ->
        ((value as? Pair<*, *>)?.first as? CoroutineScope)?.cancel()
        remove.invoke(dependencies, key)
    }
}
