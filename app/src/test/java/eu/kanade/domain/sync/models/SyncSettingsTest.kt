package eu.kanade.domain.sync.models

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SyncSettingsTest {

    @Test
    fun defaultsExcludePrivate() {
        val settings = SyncSettings()
        settings.libraryEntries shouldBe true
        settings.categories shouldBe true
        settings.chapters shouldBe true
        settings.tracking shouldBe true
        settings.history shouldBe true
        settings.appSettings shouldBe true
        settings.extensionStores shouldBe true
        settings.sourceSettings shouldBe true
        settings.privateSettings shouldBe false
        settings.customInfo shouldBe true
        settings.readEntries shouldBe true
        settings.savedSearches shouldBe true
    }

    @Test
    fun isAValue() {
        val settings = SyncSettings(privateSettings = true)
        settings shouldBe SyncSettings().copy(privateSettings = true)
        settings.hashCode() shouldBe SyncSettings(privateSettings = true).hashCode()
        settings.toString() shouldBe SyncSettings(privateSettings = true).toString()
        settings.component9() shouldBe true
    }
}
