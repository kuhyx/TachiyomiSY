package eu.kanade.tachiyomi.data.backup.create

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

private val allOff = BackupOptions(
    libraryEntries = false,
    categories = false,
    chapters = false,
    tracking = false,
    history = false,
    readEntries = false,
    appSettings = false,
    extensionStores = false,
    sourceSettings = false,
    privateSettings = false,
    customInfo = false,
    savedSearches = false,
)

internal class BackupOptionsTest {

    @Test
    fun defaults() {
        val options = BackupOptions()
        options.libraryEntries shouldBe true
        options.categories shouldBe true
        options.chapters shouldBe true
        options.tracking shouldBe true
        options.history shouldBe true
        options.readEntries shouldBe true
        options.appSettings shouldBe true
        options.extensionStores shouldBe true
        options.sourceSettings shouldBe true
        options.privateSettings shouldBe false
        options.customInfo shouldBe true
        options.savedSearches shouldBe true
    }

    @Test
    fun dataClassMembers() {
        BackupOptions() shouldBe BackupOptions()
        BackupOptions() shouldNotBe allOff
        BackupOptions().hashCode() shouldBe BackupOptions().hashCode()
        BackupOptions().toString() shouldNotBe ""
        BackupOptions().copy(categories = false).categories shouldBe false
    }

    @Test
    fun booleanArrayRoundTrip() {
        val array = BackupOptions().asBooleanArray()
        array.size shouldBe 12
        BackupOptions.fromBooleanArray(array) shouldBe BackupOptions()
        BackupOptions.fromBooleanArray(allOff.asBooleanArray()) shouldBe allOff
    }

    @Test
    fun libraryEntryGettersAndSetters() {
        BackupOptions.libraryOptions.size shouldBe 8
        BackupOptions.libraryOptions.forEach { entry ->
            entry.getter(BackupOptions()) shouldBe true
            entry.getter(allOff) shouldBe false
            entry.setter(allOff, true) shouldNotBe allOff
            entry.label shouldNotBe null
        }
    }

    @Test
    fun libraryEntryEnabledFlags() {
        BackupOptions.libraryOptions.forEach { entry ->
            entry.enabled(BackupOptions()) shouldBe true
        }
        val withoutLibrary = BackupOptions(libraryEntries = false)
        BackupOptions.libraryOptions.count { it.enabled(withoutLibrary) } shouldBe 3
    }

    @Test
    fun settingsEntryGettersAndSetters() {
        BackupOptions.settingsOptions.size shouldBe 4
        BackupOptions.settingsOptions.forEach { entry ->
            entry.getter(allOff) shouldBe false
            entry.setter(allOff, true) shouldNotBe allOff
            entry.label shouldNotBe null
        }
        BackupOptions.settingsOptions.first().getter(BackupOptions()) shouldBe true
    }

    @Test
    fun privateSettingsEnabledRule() {
        val private = BackupOptions.settingsOptions.last()
        private.enabled(BackupOptions(appSettings = true, sourceSettings = false)) shouldBe true
        private.enabled(BackupOptions(appSettings = false, sourceSettings = true)) shouldBe true
        private.enabled(BackupOptions(appSettings = false, sourceSettings = false)) shouldBe false
    }

    @Test
    fun entryEnabledDefaultsToTrue() {
        val entry = BackupOptions.Entry(
            label = BackupOptions.libraryOptions.first().label,
            getter = BackupOptions::categories,
            setter = { options, enabled -> options.copy(categories = enabled) },
        )
        entry.enabled(allOff) shouldBe true
        entry.getter(BackupOptions()) shouldBe true
        entry.setter(allOff, true).categories shouldBe true
    }

    @Test
    fun canCreateWithAnySection() {
        allOff.canCreate() shouldBe false
        allOff.copy(libraryEntries = true).canCreate() shouldBe true
        allOff.copy(categories = true).canCreate() shouldBe true
        allOff.copy(appSettings = true).canCreate() shouldBe true
        allOff.copy(extensionStores = true).canCreate() shouldBe true
        allOff.copy(sourceSettings = true).canCreate() shouldBe true
        allOff.copy(savedSearches = true).canCreate() shouldBe true
    }

    @Test
    fun entryDataClassMembers() {
        val entry = BackupOptions.libraryOptions.first()
        entry shouldBe entry.copy()
        entry shouldNotBe BackupOptions.libraryOptions.last()
        entry.hashCode() shouldBe entry.copy().hashCode()
        entry.toString() shouldNotBe ""
    }
}
