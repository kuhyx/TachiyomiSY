package exh.debug

import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions

/** The debug menu's function groups; every public function of every group is one menu entry. */
internal object DebugFunctions {
    private val groups: List<Any> = listOf(
        DebugMigrationFunctions,
        DebugEhFunctions,
        DebugSourceFunctions,
        DebugDatabaseFunctions,
        DebugJobFunctions,
    )

    /** Every menu entry with the object it must be called on. */
    fun entries(): List<Entry> = groups.flatMap { owner ->
        owner::class.declaredFunctions
            .filter { it.visibility == KVisibility.PUBLIC }
            .map { Entry(owner, it) }
    }

    /** One debug-menu function and the object it is called on. */
    data class Entry(val owner: Any, val function: KFunction<*>)
}
