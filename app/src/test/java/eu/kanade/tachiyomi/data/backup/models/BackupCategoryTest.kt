package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category

internal class BackupCategoryTest {

    @Test
    fun defaultsAreZero() {
        val category = BackupCategory(name = "Reading")
        category.name shouldBe "Reading"
        category.order shouldBe 0L
        category.id shouldBe 0L
        category.flags shouldBe 0L
        category.version shouldBe 0L
        category.uid shouldBe 0L
        category.lastModifiedAt shouldBe 0L
    }

    @Test
    fun propertiesAreMutable() {
        val category = BackupCategory(name = "Reading")
        category.name = "Done"
        category.order = 1L
        category.id = 2L
        category.flags = 3L
        category.version = 4L
        category.uid = 5L
        category.lastModifiedAt = 6L
        category.name shouldBe "Done"
        category.order shouldBe 1L
        category.uid shouldBe 5L
    }

    @Test
    fun toCategoryUsesGivenId() {
        val category = BackupCategory(
            name = "Reading",
            order = 2L,
            id = 3L,
            flags = 4L,
            version = 5L,
            uid = 6L,
            lastModifiedAt = 7L,
        )
        category.toCategory(99L) shouldBe Category(
            id = 99L,
            name = "Reading",
            order = 2L,
            flags = 4L,
            version = 5L,
            uid = 6L,
            lastModifiedAt = 7L,
        )
    }

    @Test
    fun mapperCopiesEveryField() {
        val domain = Category(
            id = 1L,
            name = "Fav",
            order = 2L,
            flags = 3L,
            version = 4L,
            uid = 5L,
            lastModifiedAt = 6L,
        )
        val backup = backupCategoryMapper(domain)
        backup.id shouldBe 1L
        backup.name shouldBe "Fav"
        backup.order shouldBe 2L
        backup.flags shouldBe 3L
        backup.version shouldBe 4L
        backup.uid shouldBe 5L
        backup.lastModifiedAt shouldBe 6L
    }

    @Test
    fun protoRoundTrip() {
        val category = BackupCategory(name = "Reading", order = 1L, id = 2L, flags = 3L)
        val bytes = ProtoBuf.encodeToByteArray(BackupCategory.serializer(), category)
        val decoded = ProtoBuf.decodeFromByteArray(BackupCategory.serializer(), bytes)
        decoded.name shouldBe "Reading"
        decoded.order shouldBe 1L
        decoded.id shouldBe 2L
        decoded.flags shouldBe 3L
    }

    @Test
    fun protoMinimalPayload() {
        val bytes = ProtoBuf.encodeToByteArray(BackupCategory.serializer(), BackupCategory(name = "N"))
        val decoded = ProtoBuf.decodeFromByteArray(BackupCategory.serializer(), bytes)
        decoded.name shouldBe "N"
        decoded.version shouldBe 0L
        decoded.lastModifiedAt shouldBe 0L
    }
}
