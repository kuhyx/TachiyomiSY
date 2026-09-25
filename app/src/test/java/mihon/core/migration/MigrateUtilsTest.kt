package mihon.core.migration

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Database
import tachiyomi.data.EhQueries

internal class MigrateUtilsTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun updateSourceIdNeedsADatabase() {
        startKoin { }
        MigrateUtils.updateSourceId(MigrationContext(dryrun = false, previousVersion = 0), newId = 2, oldId = 1)
    }

    @Test
    fun updateSourceIdRewritesRows() {
        val queries = mockk<EhQueries>(relaxed = true)
        val database = mockk<Database> { every { ehQueries } returns queries }
        startKoin { modules(module { single { database } }) }
        MigrateUtils.updateSourceId(MigrationContext(dryrun = false, previousVersion = 0), newId = 2, oldId = 1)
        coVerify(exactly = 1) { queries.migrateSource(2, 1) }
    }

    @Test
    fun replacesEveryPreferenceType() {
        val written = mutableMapOf<String, Preference<*>>()
        val deleted = mutableListOf<String>()
        val store = mockk<PreferenceStore>()
        every { store.getAll() } returns mapOf(
            "int" to 1,
            "long" to 2L,
            "float" to 3f,
            "string" to "s",
            "boolean" to true,
            "set" to setOf("a"),
            "double" to 4.0,
            "skipped" to 5,
        )
        every { store.getInt(any(), any()) } answers { record(written, deleted, firstArg(), 0) }
        every { store.getLong(any(), any()) } answers { record(written, deleted, firstArg(), 0L) }
        every { store.getFloat(any(), any()) } answers { record(written, deleted, firstArg(), 0f) }
        every { store.getString(any(), any()) } answers { record(written, deleted, firstArg(), "") }
        every { store.getBoolean(any(), any()) } answers { record(written, deleted, firstArg(), false) }
        every { store.getStringSet(any(), any()) } answers { record(written, deleted, firstArg(), emptySet()) }

        MigrateUtils.replacePreferences(store, { it.key != "skipped" }) { "new_$it" }

        written.mapValues { it.value.get() } shouldBe mapOf(
            "new_int" to 1,
            "new_long" to 2L,
            "new_float" to 3f,
            "new_string" to "s",
            "new_boolean" to true,
            "new_set" to setOf("a"),
        )
        deleted shouldBe listOf("int", "long", "float", "string", "boolean", "set")
    }

    @Test
    fun inMemoryStoreHasNothing() {
        val store = InMemoryPreferenceStore(sequenceOf(InMemoryPreference("a", 1, 0)))
        MigrateUtils.replacePreferences(store, { true }) { it }
        store.getInt("a").get() shouldBe 1
    }

    // A preference that remembers the key it was written under and the delete it received.
    private fun <T> record(
        written: MutableMap<String, Preference<*>>,
        deleted: MutableList<String>,
        key: String,
        default: T,
    ): Preference<T> = object : Preference<T> {
        override fun key(): String = key

        override fun get(): T = default

        override fun set(value: T) {
            written[key] = InMemoryPreference(key, value, default)
        }

        override fun isSet(): Boolean = false

        override fun delete() {
            deleted += key
        }

        override fun defaultValue(): T = default

        override fun changes(): Flow<T> = emptyFlow()

        override fun stateIn(scope: CoroutineScope): StateFlow<T> = MutableStateFlow(default)
    }
}
