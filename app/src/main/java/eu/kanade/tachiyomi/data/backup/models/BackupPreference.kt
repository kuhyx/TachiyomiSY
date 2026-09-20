package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class BackupPreference(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val value: PreferenceValue,
)

@Serializable
internal data class BackupSourcePreferences(
    @ProtoNumber(1) val sourceKey: String,
    @ProtoNumber(2) val prefs: List<BackupPreference>,
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
