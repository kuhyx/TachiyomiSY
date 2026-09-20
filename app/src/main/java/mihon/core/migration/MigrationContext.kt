package mihon.core.migration

import uy.kohesive.injekt.Injekt

internal class MigrationContext(
    val dryrun: Boolean,
    val previousVersion: Int,
) {

    inline fun <reified T> get(): T? = Injekt.getInstanceOrNull(T::class.java)

    // The registered instance, or IllegalStateException when the app forgot to register it.
    inline fun <reified T> require(): T = checkNotNull(get<T>()) { "${T::class.java.name} is not registered" }
}
