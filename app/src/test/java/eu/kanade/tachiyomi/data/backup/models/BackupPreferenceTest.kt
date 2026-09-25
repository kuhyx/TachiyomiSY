package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test

internal class BackupPreferenceTest {

    @Test
    fun holdsKeyAndValue() {
        val preference = BackupPreference(key = "k", value = IntPreferenceValue(1))
        preference.key shouldBe "k"
        preference.value shouldBe IntPreferenceValue(1)
        preference shouldBe BackupPreference(key = "k", value = IntPreferenceValue(1))
        preference shouldNotBe BackupPreference(key = "j", value = IntPreferenceValue(1))
        preference.hashCode() shouldNotBe 0
        preference.toString() shouldNotBe ""
        preference.copy(key = "j").key shouldBe "j"
    }

    @Test
    fun sourcePreferencesHoldsPrefs() {
        val prefs = listOf(BackupPreference(key = "k", value = BooleanPreferenceValue(true)))
        val source = BackupSourcePreferences(sourceKey = "s", prefs = prefs)
        source.sourceKey shouldBe "s"
        source.prefs shouldBe prefs
        source shouldBe BackupSourcePreferences(sourceKey = "s", prefs = prefs)
        source shouldNotBe BackupSourcePreferences(sourceKey = "t", prefs = prefs)
        source.hashCode() shouldNotBe 0
        source.toString() shouldNotBe ""
        source.copy(sourceKey = "t").sourceKey shouldBe "t"
    }

    @Test
    fun everyValueSubclass() {
        IntPreferenceValue(1).value shouldBe 1
        LongPreferenceValue(2L).value shouldBe 2L
        FloatPreferenceValue(3F).value shouldBe 3F
        StringPreferenceValue("s").value shouldBe "s"
        BooleanPreferenceValue(true).value shouldBe true
        StringSetPreferenceValue(setOf("a")).value shouldBe setOf("a")
    }

    @Test
    fun valueSubclassMembers() {
        IntPreferenceValue(1) shouldBe IntPreferenceValue(1)
        IntPreferenceValue(1) shouldNotBe IntPreferenceValue(2)
        IntPreferenceValue(1).hashCode() shouldBe IntPreferenceValue(1).hashCode()
        IntPreferenceValue(1).toString() shouldNotBe ""
        IntPreferenceValue(1).copy(value = 5).value shouldBe 5
        LongPreferenceValue(2L).copy(value = 6L) shouldBe LongPreferenceValue(6L)
        FloatPreferenceValue(3F).copy(value = 7F) shouldBe FloatPreferenceValue(7F)
        StringPreferenceValue("s").copy(value = "t") shouldBe StringPreferenceValue("t")
        BooleanPreferenceValue(true).copy(value = false) shouldBe BooleanPreferenceValue(false)
        StringSetPreferenceValue(setOf("a")).copy(value = setOf("b")) shouldBe StringSetPreferenceValue(setOf("b"))
    }

    @Test
    fun valueSubclassEqualityAndHash() {
        LongPreferenceValue(2L).hashCode() shouldBe LongPreferenceValue(2L).hashCode()
        LongPreferenceValue(2L) shouldNotBe LongPreferenceValue(3L)
        FloatPreferenceValue(3F).hashCode() shouldBe FloatPreferenceValue(3F).hashCode()
        FloatPreferenceValue(3F) shouldNotBe FloatPreferenceValue(4F)
        StringPreferenceValue("s").hashCode() shouldBe StringPreferenceValue("s").hashCode()
        StringPreferenceValue("s") shouldNotBe StringPreferenceValue("t")
        BooleanPreferenceValue(true).hashCode() shouldBe BooleanPreferenceValue(true).hashCode()
        BooleanPreferenceValue(true) shouldNotBe BooleanPreferenceValue(false)
        StringSetPreferenceValue(setOf("a")).hashCode() shouldBe StringSetPreferenceValue(setOf("a")).hashCode()
        StringSetPreferenceValue(setOf("a")) shouldNotBe StringSetPreferenceValue(setOf("b"))
        LongPreferenceValue(2L).toString() shouldNotBe ""
        FloatPreferenceValue(3F).toString() shouldNotBe ""
        StringPreferenceValue("s").toString() shouldNotBe ""
        BooleanPreferenceValue(true).toString() shouldNotBe ""
        StringSetPreferenceValue(setOf("a")).toString() shouldNotBe ""
    }

    @Test
    fun protoRoundTrip() {
        val source = BackupSourcePreferences(
            sourceKey = "s",
            prefs = listOf(
                BackupPreference(key = "i", value = IntPreferenceValue(1)),
                BackupPreference(key = "l", value = LongPreferenceValue(2L)),
                BackupPreference(key = "f", value = FloatPreferenceValue(3F)),
                BackupPreference(key = "s", value = StringPreferenceValue("v")),
                BackupPreference(key = "b", value = BooleanPreferenceValue(true)),
                BackupPreference(key = "ss", value = StringSetPreferenceValue(setOf("a", "b"))),
            ),
        )
        val bytes = ProtoBuf.encodeToByteArray(BackupSourcePreferences.serializer(), source)
        ProtoBuf.decodeFromByteArray(BackupSourcePreferences.serializer(), bytes) shouldBe source
    }
}
