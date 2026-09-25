package eu.kanade.tachiyomi.data.sync.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

private val allTriggers = SyncTriggerOptions(
    syncOnChapterRead = true,
    syncOnChapterOpen = true,
    syncOnAppStart = true,
    syncOnAppResume = true,
)

internal class SyncTriggerOptionsTest {

    @Test
    fun defaultsAreAllOff() {
        val options = SyncTriggerOptions()
        options.syncOnChapterRead shouldBe false
        options.syncOnChapterOpen shouldBe false
        options.syncOnAppStart shouldBe false
        options.syncOnAppResume shouldBe false
    }

    @Test
    fun dataClassMembers() {
        SyncTriggerOptions() shouldBe SyncTriggerOptions()
        SyncTriggerOptions() shouldNotBe allTriggers
        SyncTriggerOptions().hashCode() shouldBe SyncTriggerOptions().hashCode()
        SyncTriggerOptions().toString() shouldNotBe ""
        SyncTriggerOptions().copy(syncOnAppStart = true).syncOnAppStart shouldBe true
    }

    @Test
    fun booleanArrayRoundTrip() {
        val array = allTriggers.asBooleanArray()
        array.size shouldBe 4
        SyncTriggerOptions.fromBooleanArray(array) shouldBe allTriggers
        SyncTriggerOptions.fromBooleanArray(SyncTriggerOptions().asBooleanArray()) shouldBe SyncTriggerOptions()
    }

    @Test
    fun entryGettersAndSetters() {
        SyncTriggerOptions.mainOptions.size shouldBe 4
        SyncTriggerOptions.mainOptions.forEach { entry ->
            entry.getter(allTriggers) shouldBe true
            entry.getter(SyncTriggerOptions()) shouldBe false
            entry.setter(SyncTriggerOptions(), true) shouldNotBe SyncTriggerOptions()
            entry.enabled(SyncTriggerOptions()) shouldBe true
            entry.label shouldNotBe null
        }
    }

    @Test
    fun anyEnabledPerTrigger() {
        SyncTriggerOptions().anyEnabled() shouldBe false
        SyncTriggerOptions(syncOnChapterRead = true).anyEnabled() shouldBe true
        SyncTriggerOptions(syncOnChapterOpen = true).anyEnabled() shouldBe true
        SyncTriggerOptions(syncOnAppStart = true).anyEnabled() shouldBe true
        SyncTriggerOptions(syncOnAppResume = true).anyEnabled() shouldBe true
    }

    @Test
    fun entryDataClassMembers() {
        val entry = SyncTriggerOptions.mainOptions.first()
        entry shouldBe entry.copy()
        entry shouldNotBe SyncTriggerOptions.mainOptions.last()
        entry.hashCode() shouldBe entry.copy().hashCode()
        entry.toString() shouldNotBe ""
    }
}
