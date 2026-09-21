package eu.kanade.tachiyomi.data.backup.restore.restorers

import android.content.Context
import android.util.Log
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.PreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.source.sourcePreferences
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class PreferenceRestorer(
    private val context: Context,
    private val getCategories: GetCategories = Injekt.get(),
    private val preferenceStore: PreferenceStore = Injekt.get(),
) {
    suspend fun restoreApp(
        preferences: List<BackupPreference>,
        backupCategories: List<BackupCategory>?,
    ) {
        restorePreferences(
            preferences,
            preferenceStore,
            backupCategories,
        )

        LibraryUpdateJob.setupTask(context)
        BackupCreateJob.setupTask(context)
    }

    suspend fun restoreSource(preferences: List<BackupSourcePreferences>) {
        preferences.forEach {
            val sourcePrefs = AndroidPreferenceStore(context, sourcePreferences(it.sourceKey))
            restorePreferences(it.prefs, sourcePrefs)
        }
    }

    private suspend fun restorePreferences(
        toRestore: List<BackupPreference>,
        preferenceStore: PreferenceStore,
        backupCategories: List<BackupCategory>? = null,
    ) {
        val allCategories = if (backupCategories != null) getCategories.await() else emptyList()
        val mapping = CategoryMapping(
            backupCategoriesById = backupCategories?.associateBy { it.id.toString() }.orEmpty(),
            categoriesByName = allCategories.associateBy { it.name },
        )
        val prefs = preferenceStore.getAll()
        toRestore.forEach { (key, value) ->
            try {
                restoreOne(key, value, current = prefs[key], preferenceStore, mapping)
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                Log.e("PreferenceRestorer", "Failed to restore preference <$key>", expected)
            }
        }
    }

    // A value is only written over a stored value of the same type (or none at all).
    private fun restoreOne(
        key: String,
        value: PreferenceValue,
        current: Any?,
        preferenceStore: PreferenceStore,
        mapping: CategoryMapping,
    ) {
        when (value) {
            is IntPreferenceValue -> if (current is Int?) restoreInt(key, value.value, preferenceStore, mapping)
            is LongPreferenceValue -> if (current is Long?) preferenceStore.getLong(key).set(value.value)
            is FloatPreferenceValue -> if (current is Float?) preferenceStore.getFloat(key).set(value.value)
            is StringPreferenceValue -> if (current is String?) preferenceStore.getString(key).set(value.value)
            is BooleanPreferenceValue -> if (current is Boolean?) preferenceStore.getBoolean(key).set(value.value)
            is StringSetPreferenceValue -> if (current is Set<*>?) {
                restoreStringSet(key, value.value, preferenceStore, mapping)
            }
        }
    }

    // The default category is stored by id, which the backup's ids must be mapped onto.
    private fun restoreInt(key: String, value: Int, preferenceStore: PreferenceStore, mapping: CategoryMapping) {
        val newValue = if (key == LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY) {
            mapping.localCategoryId(value.toString())?.toInt()
        } else {
            value
        }
        newValue?.let { preferenceStore.getInt(key).set(it) }
    }

    private fun restoreStringSet(
        key: String,
        value: Set<String>,
        preferenceStore: PreferenceStore,
        mapping: CategoryMapping,
    ) {
        val restored = restoreCategoriesPreference(key, value, preferenceStore, mapping)
        if (!restored) preferenceStore.getStringSet(key).set(value)
    }

    private fun restoreCategoriesPreference(
        key: String,
        value: Set<String>,
        preferenceStore: PreferenceStore,
        mapping: CategoryMapping,
    ): Boolean {
        val categoryPreferences = LibraryPreferences.categoryPreferenceKeys + DownloadPreferences.categoryPreferenceKeys
        if (key !in categoryPreferences) return false

        val ids = value.mapNotNull { mapping.localCategoryId(it)?.toString() }

        if (ids.isNotEmpty()) {
            preferenceStore.getStringSet(key) += ids
        }
        return true
    }
}

// A backup's category ids map onto the local category of the same name.
private class CategoryMapping(
    private val backupCategoriesById: Map<String, BackupCategory>,
    private val categoriesByName: Map<String, Category>,
) {
    fun localCategoryId(backupId: String): Long? = backupCategoriesById[backupId]?.let { categoriesByName[it.name]?.id }
}
