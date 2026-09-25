package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.backup.models.metadata.BackupSearchMetadata
import eu.kanade.tachiyomi.data.backup.models.metadata.BackupSearchTag
import eu.kanade.tachiyomi.data.backup.models.metadata.BackupSearchTitle

/** The metadata blob every flat-metadata fixture below shares. */
internal val backupSearchMetadata: BackupSearchMetadata = BackupSearchMetadata(
    uploader = "u",
    extra = "{}",
    indexedExtra = "i",
    extraVersion = 1,
)

/** A flat metadata with exactly one tag and one title, so `single()` is usable on both. */
internal fun backupFlatMetadata(): BackupFlatMetadata = BackupFlatMetadata(
    searchMetadata = backupSearchMetadata,
    searchTags = listOf(BackupSearchTag(namespace = "ns", name = "n", type = 2)),
    searchTitles = listOf(BackupSearchTitle(title = "t", type = 3)),
)

/** A merged reference with every flag set to a distinguishable value. */
internal fun backupMergedMangaReference(): BackupMergedMangaReference = BackupMergedMangaReference(
    isInfoManga = true,
    getChapterUpdates = false,
    chapterSortMode = 1,
    chapterPriority = 2,
    downloadChapters = true,
    mergeUrl = "merge://1",
    mangaUrl = "/m",
    mangaSourceId = 3L,
)

/** An extension store whose nullable fields are all filled unless overridden. */
internal fun backupExtensionStore(
    badgeLabel: String? = "badge",
    contactDiscord: String? = "discord",
    isLegacy: Boolean? = false,
    extensionListUrl: String? = "https://list",
): BackupExtensionStore = BackupExtensionStore(
    indexUrl = "https://index",
    name = "Store",
    badgeLabel = badgeLabel,
    signingKey = "key",
    contactWebsite = "https://site",
    contactDiscord = contactDiscord,
    isLegacy = isLegacy,
    extensionListUrl = extensionListUrl,
)
