package eu.kanade.tachiyomi.data.sync.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.sy.SYMR

internal class SyncTriggerOptionsTest {

    private val all = SyncTriggerOptions(
        syncOnChapterRead = true,
        syncOnChapterOpen = true,
        syncOnAppStart = true,
        syncOnAppResume = true,
    )

    @Test
    fun defaultsAreAllOff() {
        val options = SyncTriggerOptions()
        options.asBooleanArray().toList() shouldBe listOf(false, false, false, false)
        options.anyEnabled() shouldBe false
    }

    @Test
    fun anyEnabledChecksEveryFlag() {
        SyncTriggerOptions(syncOnChapterRead = true).anyEnabled() shouldBe true
        SyncTriggerOptions(syncOnChapterOpen = true).anyEnabled() shouldBe true
        SyncTriggerOptions(syncOnAppStart = true).anyEnabled() shouldBe true
        SyncTriggerOptions(syncOnAppResume = true).anyEnabled() shouldBe true
    }

    @Test
    fun booleanArrayRoundTrip() {
        val array = booleanArrayOf(true, false, true, false)
        val options = SyncTriggerOptions.fromBooleanArray(array)
        options shouldBe SyncTriggerOptions(syncOnChapterRead = true, syncOnAppStart = true)
        options.asBooleanArray().toList() shouldBe array.toList()
    }

    @Test
    fun mainOptionsGetAndSetEachFlag() {
        val entries = SyncTriggerOptions.mainOptions
        entries.map { it.label } shouldBe listOf(
            SYMR.strings.sync_on_chapter_read,
            SYMR.strings.sync_on_chapter_open,
            SYMR.strings.sync_on_app_start,
            SYMR.strings.sync_on_app_resume,
        )
        entries.forEach { entry ->
            val enabled = entry.setter(SyncTriggerOptions(), true)
            entry.getter(enabled) shouldBe true
            entry.getter(entry.setter(all, false)) shouldBe false
            entry.enabled(enabled) shouldBe true
        }
    }

    @Test
    fun dataClassMembers() {
        val copy = all.copy(syncOnAppResume = false)
        copy shouldNotBe all
        copy.hashCode() shouldBe SyncTriggerOptions(
            syncOnChapterRead = true,
            syncOnChapterOpen = true,
            syncOnAppStart = true,
        ).hashCode()
        copy.toString() shouldBe "SyncTriggerOptions(syncOnChapterRead=true, syncOnChapterOpen=true, " +
            "syncOnAppStart=true, syncOnAppResume=false)"
        val (read, open, start) = copy
        listOf(read, open, start) shouldBe listOf(true, true, true)
        copy.component4() shouldBe false
    }

    @Test
    fun entryDataClassMembers() {
        val entry = SyncTriggerOptions.mainOptions.first()
        val copy = entry.copy(enabled = { false })
        copy.enabled(all) shouldBe false
        copy shouldNotBe entry
        entry.copy() shouldBe entry
        entry.hashCode() shouldBe entry.copy().hashCode()
        entry.toString().startsWith("Entry(label=") shouldBe true
        val (label, getter, setter) = entry
        label shouldBe SYMR.strings.sync_on_chapter_read
        getter(all) shouldBe true
        setter(all, false).syncOnChapterRead shouldBe false
        entry.component4()(all) shouldBe true
    }
}
