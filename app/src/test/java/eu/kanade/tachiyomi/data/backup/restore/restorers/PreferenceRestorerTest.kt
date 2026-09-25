package eu.kanade.tachiyomi.data.backup.restore.restorers

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import eu.kanade.tachiyomi.data.backup.BackupKoin
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class PreferenceRestorerTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val graph = BackupKoin()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val restorer by lazy { PreferenceRestorer(app, graph.getCategories, graph.store) }
    private val reading = Category(id = 42L, name = "Reading", order = 0, flags = 0)
    private val everyType = listOf(
        pref("i", 1),
        pref("l", 2L),
        pref("f", 3F),
        pref("b", true),
        pref("s", "x"),
        pref("ss", setOf("a")),
    )

    @Before
    fun setUp() {
        startKoin { modules(graph.module(), module { single { app } }) }
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        every { any<Context>().workManager } returns workManager
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun pref(key: String, value: Any): BackupPreference = BackupPreference(
        key,
        when (value) {
            is Int -> IntPreferenceValue(value)
            is Long -> LongPreferenceValue(value)
            is Float -> FloatPreferenceValue(value)
            is Boolean -> BooleanPreferenceValue(value)
            is String -> StringPreferenceValue(value)
            else -> StringSetPreferenceValue((value as Set<*>).map { it.toString() }.toSet())
        },
    )

    @Test
    fun everyTypeIsRestored() = runTest {
        restorer.restoreApp(
            everyType,
            backupCategories = null,
        )
        graph.store.getInt("i", 0).get() shouldBe 1
        graph.store.getLong("l", 0L).get() shouldBe 2L
        graph.store.getFloat("f", 0F).get() shouldBe 3F
        graph.store.getBoolean("b", false).get() shouldBe true
        graph.store.getString("s", "").get() shouldBe "x"
        graph.store.getStringSet("ss", emptySet()).get() shouldBe setOf("a")
        coVerify(exactly = 0) { graph.getCategories.await() }
        verify { workManager.enqueueUniquePeriodicWork("BackupCreator", any(), any()) }
    }

    @Test
    fun mismatchedTypesAreSkipped() = runTest {
        listOf("i", "l", "f", "b", "ss").forEach { graph.store.getString(it, "").set("text") }
        graph.store.getInt("s", 0).set(7)
        restorer.restoreApp(
            everyType,
            backupCategories = null,
        )
        graph.store.getInt("i", 0).get() shouldBe 0
        graph.store.getLong("l", 0L).get() shouldBe 0L
        graph.store.getFloat("f", 0F).get() shouldBe 0F
        graph.store.getBoolean("b", false).get() shouldBe false
        graph.store.getStringSet("ss", emptySet()).get() shouldBe emptySet()
        graph.store.getString("s", "").get() shouldBe ""
    }

    @Test
    fun defaultCategoryIsMappedByName() = runTest {
        coEvery { graph.getCategories.await() } returns listOf(reading)
        val backupCategories = listOf(BackupCategory(name = "Reading", id = 5), BackupCategory(name = "Gone", id = 6))
        restorer.restoreApp(listOf(pref("default_category", 5)), backupCategories)
        graph.store.getInt("default_category", -1).get() shouldBe 42
        restorer.restoreApp(listOf(pref("default_category", 6)), backupCategories)
        graph.store.getInt("default_category", -1).get() shouldBe 42
        restorer.restoreApp(listOf(pref("default_category", 9)), backupCategories)
        graph.store.getInt("default_category", -1).get() shouldBe 42
    }

    @Test
    fun categorySetsAreMappedByName() = runTest {
        coEvery { graph.getCategories.await() } returns listOf(reading)
        val backupCategories = listOf(BackupCategory(name = "Reading", id = 5))
        graph.store.getStringSet("library_update_categories", emptySet()).set(setOf("1"))
        restorer.restoreApp(listOf(pref("library_update_categories", setOf("5", "8"))), backupCategories)
        graph.store.getStringSet("library_update_categories", emptySet()).get() shouldBe setOf("1", "42")
        restorer.restoreApp(listOf(pref("download_new_categories", setOf("8"))), backupCategories)
        graph.store.getStringSet("download_new_categories", setOf("x")).get() shouldBe setOf("x")
    }

    @Test
    fun sourcePrefsGoToTheirFile() = runTest {
        restorer.restoreSource(listOf(BackupSourcePreferences("source_7", listOf(pref("lang", "en")))))
        AndroidPreferenceStore(app, app.getSharedPreferences("source_7", Context.MODE_PRIVATE))
            .getString("lang", "")
            .get() shouldBe "en"
    }

    @Test
    fun failuresSkipOnlyThatKey() = runTest {
        val broken = mockk<PreferenceStore> {
            every { getAll() } returns emptyMap<String, Any>()
            every { getInt("bad", any()) } throws IllegalStateException("locked")
            every { getString("good", any()) } returns graph.store.getString("good", "")
        }
        PreferenceRestorer(app, graph.getCategories, broken).restoreApp(
            listOf(pref("bad", 1), pref("good", "ok")),
            backupCategories = null,
        )
        graph.store.getString("good", "").get() shouldBe "ok"
    }
}
