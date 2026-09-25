package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import java.lang.reflect.Method
import java.lang.reflect.Modifier

private const val SLOTS_PER_INT = 10
private const val BITS_PER_SLOT = 3
private const val BITS_PER_INT = 31
private const val UNSTABLE_BIT = 0b100

/**
 * Calls the composable [name] of the file facade (or object) [owner] with [args] (an extension
 * receiver first) as a caller passing the "unstable" stability bit for every argument would.
 *
 * Under strong skipping no Kotlin call site originates that bit: it only forwards it from a generic
 * parent's `$dirty` or reads it from another module's `$stable` field. The compiler still branches on
 * it wherever a memoized lambda captures an interface-typed parameter (`changedInstance`), so this is
 * the only way a test reaches that arm. Every argument is passed; default masks are all zero.
 */
@Composable
internal fun CallWithUnstableBits(owner: Class<*>, name: String, args: List<Any?>) {
    val changedInts = maxOf(1, (args.size + SLOTS_PER_INT - 1) / SLOTS_PER_INT)
    val defaultInts = (args.size + BITS_PER_INT - 1) / BITS_PER_INT
    val method = owner.composable(name, args.size + 1 + changedInts, args.size + 1 + changedInts + defaultInts)
    val changed = IntArray(changedInts)
    args.indices.forEach { slot ->
        changed[slot / SLOTS_PER_INT] = changed[slot / SLOTS_PER_INT] or
            (UNSTABLE_BIT shl slot % SLOTS_PER_INT * BITS_PER_SLOT + 1)
    }
    val trailing = changed.toList() + List(method.parameterCount - args.size - 1 - changedInts) { 0 }
    val receiver = if (Modifier.isStatic(method.modifiers)) null else owner.getField("INSTANCE").get(null)
    method.invoke(receiver, *args.toTypedArray(), currentComposer, *trailing.toTypedArray())
}

private fun Class<*>.composable(name: String, vararg arities: Int): Method =
    declaredMethods.single { method ->
        (method.name == name || method.name.startsWith("$name-")) && method.parameterCount in arities
    }.apply { isAccessible = true }
