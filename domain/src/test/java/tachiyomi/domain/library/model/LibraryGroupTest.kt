package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

internal class LibraryGroupTest {

    @Test
    fun constantsAreStable() {
        LibraryGroup.BY_DEFAULT shouldBe 0
        LibraryGroup.BY_SOURCE shouldBe 1
        LibraryGroup.BY_STATUS shouldBe 2
        LibraryGroup.BY_TRACK_STATUS shouldBe 3
        LibraryGroup.UNGROUPED shouldBe 4
    }

    @Test
    fun namedGroupsHaveOwnLabel() {
        LibraryGroup.groupTypeStringRes(LibraryGroup.BY_STATUS) shouldBe MR.strings.status
        LibraryGroup.groupTypeStringRes(LibraryGroup.BY_SOURCE) shouldBe MR.strings.label_sources
        LibraryGroup.groupTypeStringRes(LibraryGroup.BY_TRACK_STATUS) shouldBe SYMR.strings.tracking_status
        LibraryGroup.groupTypeStringRes(LibraryGroup.UNGROUPED) shouldBe SYMR.strings.ungrouped
    }

    @Test
    fun defaultLabelTracksCategories() {
        LibraryGroup.groupTypeStringRes(LibraryGroup.BY_DEFAULT) shouldBe MR.strings.categories
        LibraryGroup.groupTypeStringRes(LibraryGroup.BY_DEFAULT, hasCategories = true) shouldBe MR.strings.categories
        LibraryGroup.groupTypeStringRes(LibraryGroup.BY_DEFAULT, hasCategories = false) shouldBe SYMR.strings.ungrouped
    }

    @Test
    fun unknownTypeIsDefault() {
        LibraryGroup.groupTypeStringRes(99) shouldBe MR.strings.categories
        LibraryGroup.groupTypeStringRes(99, hasCategories = false) shouldBe SYMR.strings.ungrouped
    }
}
