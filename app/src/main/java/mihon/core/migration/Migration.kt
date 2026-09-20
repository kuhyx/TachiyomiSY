package mihon.core.migration

internal interface Migration {
    val version: Float

    val isAlways: Boolean
        get() = version == ALWAYS

    suspend operator fun invoke(migrationContext: MigrationContext): Boolean

    companion object {
        const val ALWAYS = -1f
    }
}
