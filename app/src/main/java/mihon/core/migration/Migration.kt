package mihon.core.migration

internal interface Migration {
    val version: Float

    val isAlways: Boolean
        get() = version == ALWAYS

    suspend operator fun invoke(migrationContext: MigrationContext): Boolean

    companion object {
        const val ALWAYS = -1f

        fun of(version: Float, action: suspend (MigrationContext) -> Boolean): Migration = object : Migration {
            override val version: Float = version

            override suspend operator fun invoke(migrationContext: MigrationContext): Boolean = action(migrationContext)
        }
    }
}
