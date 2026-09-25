package mihon.feature.common.utils

import io.kotest.matchers.shouldBe
import mihon.domain.migration.models.MigrationFlag
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class MigrationFlagTest {
    @Test
    fun everyFlagHasALabel() {
        MigrationFlag.CHAPTER.getLabel() shouldBe MR.strings.chapters
        MigrationFlag.CATEGORY.getLabel() shouldBe MR.strings.categories
        MigrationFlag.CUSTOM_COVER.getLabel() shouldBe MR.strings.custom_cover
        MigrationFlag.NOTES.getLabel() shouldBe MR.strings.action_notes
        MigrationFlag.REMOVE_DOWNLOAD.getLabel() shouldBe MR.strings.delete_downloaded
    }

    @Test
    fun labelsAreDistinct() {
        MigrationFlag.entries.map { it.getLabel() }.distinct().size shouldBe MigrationFlag.entries.size
    }
}
