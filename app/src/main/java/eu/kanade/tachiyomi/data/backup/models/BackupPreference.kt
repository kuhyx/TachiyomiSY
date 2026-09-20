package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

private const val BACKUP_PREFERENCE_KEY = 1
private const val BACKUP_PREFERENCE_VALUE = 2
private const val BACKUP_SOURCE_PREFERENCES_SOURCE_KEY = 1
private const val BACKUP_SOURCE_PREFERENCES_PREFS = 2

@Serializable
internal data class BackupPreference(
    @ProtoNumber(BACKUP_PREFERENCE_KEY) val key: String,
    @ProtoNumber(BACKUP_PREFERENCE_VALUE) val value: PreferenceValue,
)

@Serializable
internal data class BackupSourcePreferences(
    @ProtoNumber(BACKUP_SOURCE_PREFERENCES_SOURCE_KEY) val sourceKey: String,
    @ProtoNumber(BACKUP_SOURCE_PREFERENCES_PREFS) val prefs: List<BackupPreference>,
)

@Serializable
internal sealed class PreferenceValue

@Serializable
internal data class IntPreferenceValue(val value: Int) : PreferenceValue()

@Serializable
internal data class LongPreferenceValue(val value: Long) : PreferenceValue()

@Serializable
internal data class FloatPreferenceValue(val value: Float) : PreferenceValue()

@Serializable
internal data class StringPreferenceValue(val value: String) : PreferenceValue()

@Serializable
internal data class BooleanPreferenceValue(val value: Boolean) : PreferenceValue()

@Serializable
internal data class StringSetPreferenceValue(val value: Set<String>) : PreferenceValue()
