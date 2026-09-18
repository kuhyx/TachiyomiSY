package tachiyomi.core.common.preference

import java.lang.reflect.Method

/**
 * A static method of a Kotlin file facade or class, looked up by exact parameter types.
 *
 * Tests reach for this to execute code the compiler never calls directly: the non-inlined
 * copy of an `inline fun` (JaCoCo credits that copy, not the call sites in test classes) and
 * deprecated overloads (a direct call is a warning, and warnings are errors).
 */
internal fun staticMethod(className: String, name: String, parameterTypes: List<Class<*>>): Method =
    Class.forName(className).getMethod(name, *parameterTypes.toTypedArray())

/** Invokes a static [Method] with [args]. */
internal fun Method.callStatic(args: List<Any?>): Any? = invoke(null, *args.toTypedArray())
