package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.category.model.Category

private const val BACKUP_CATEGORY_NAME = 1
private const val BACKUP_CATEGORY_ORDER = 2
private const val BACKUP_CATEGORY_ID = 3
private const val BACKUP_CATEGORY_FLAGS = 100
private const val BACKUP_CATEGORY_VERSION = 601
private const val BACKUP_CATEGORY_UID = 602
private const val BACKUP_CATEGORY_LAST_MODIFIED_AT = 603

@Serializable
internal class BackupCategory(
    @ProtoNumber(BACKUP_CATEGORY_NAME) var name: String,
    @ProtoNumber(BACKUP_CATEGORY_ORDER) var order: Long = 0,
    @ProtoNumber(BACKUP_CATEGORY_ID) var id: Long = 0,
    // @ProtoNumber(3) val updateInterval: Int = 0, 1.x value not used in 0.x
    @ProtoNumber(BACKUP_CATEGORY_FLAGS) var flags: Long = 0,
    // SY specific values
    /*@ProtoNumber(600) var mangaOrder: List<Long> = emptyList(),*/
    @ProtoNumber(BACKUP_CATEGORY_VERSION) var version: Long = 0,
    @ProtoNumber(BACKUP_CATEGORY_UID) var uid: Long = 0,
    @ProtoNumber(BACKUP_CATEGORY_LAST_MODIFIED_AT) var lastModifiedAt: Long = 0,
) {
    fun toCategory(id: Long) = Category(
        id = id,
        name = this@BackupCategory.name,
        flags = this@BackupCategory.flags,
        order = this@BackupCategory.order,
        version = this@BackupCategory.version,
        uid = this@BackupCategory.uid,
        lastModifiedAt = this@BackupCategory.lastModifiedAt,
        /*mangaOrder = this@BackupCategory.mangaOrder*/
    )
}

internal val backupCategoryMapper = { category: Category ->
    BackupCategory(
        id = category.id,
        name = category.name,
        order = category.order,
        flags = category.flags,
        version = category.version,
        uid = category.uid,
        lastModifiedAt = category.lastModifiedAt,
    )
}
