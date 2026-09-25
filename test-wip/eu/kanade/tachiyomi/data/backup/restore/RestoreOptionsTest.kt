package eu.kanade.tachiyomi.data.backup.restore

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

private val noneOn = RestoreOptions(
    libraryEntries = false,
    categories = false,
    appSettings = false,
    extensionStores = false,
    sourceSettings = false,
    savedSearches = false,
)

internal class RestoreOptionsTest {

    @Test
    fun defaultsAreAllOn() {
        val options = RestoreOptions()
        options.libraryEntries shouldBe true
        options.categories shouldBe true
        options.appSettings shouldBe true
        options.extensionStores shouldBe true
        options.sourceSettings shouldBe true
        options.savedSearches shouldBe true
    }

    @Test
    fun dataClassMembers() {
        RestoreOptions() shouldBe RestoreOptions()
        RestoreOptions() shouldNotBe noneOn
        RestoreOptions().hashCode() shouldBe RestoreOptions().hashCode()
        RestoreOptions().toString() shouldNotBe ""
        RestoreOptions().copy(categories = false).categories shouldBe false
    }

    @Test
    fun booleanArrayRoundTrip() {
        val array = RestoreOptions().asBooleanArray()
        array.size shouldBe 6
        RestoreOptions.fromBooleanArray(array) shouldBe RestoreOptions()
        RestoreOptions.fromBooleanArray(noneOn.asBooleanArray()) shouldBe noneOn
    }

    @Test
    fun entryGettersAndSetters() {
        RestoreOptions.options.size shouldBe 6
        RestoreOptions.options.forEach { entry ->
            entry.getter(RestoreOptions()) shouldBe true
            entry.getter(noneOn) shouldBe false
            entry.setter(noneOn, true) shouldNotBe noneOn
            entry.label shouldNotBe null
        }
    }

    @Test
    fun canRestoreWithAnySection() {
        noneOn.canRestore() shouldBe false
        noneOn.copy(libraryEntries = true).canRestore() shouldBe true
        noneOn.copy(categories = true).canRestore() shouldBe true
        noneOn.copy(appSettings = true).canRestore() shouldBe true
        noneOn.copy(extensionStores = true).canRestore() shouldBe true
        noneOn.copy(sourceSettings = true).canRestore() shouldBe true
        noneOn.copy(savedSearches = true).canRestore() shouldBe true
    }

    @Test
    fun entryDataClassMembers() {
        val entry = RestoreOptions.options.first()
        entry shouldBe entry.copy()
        entry shouldNotBe RestoreOptions.options.last()
        entry.hashCode() shouldBe entry.copy().hashCode()
        entry.toString() shouldNotBe ""
    }
}
