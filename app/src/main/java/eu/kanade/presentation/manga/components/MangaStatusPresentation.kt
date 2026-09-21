package eu.kanade.presentation.manga.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.i18n.MR

// Label and icon for each SManga publishing status; anything unknown falls back to "unknown".
internal data class MangaStatusPresentation(val label: StringResource, val icon: ImageVector)

private val statusPresentations = mapOf(
    SManga.ONGOING.toLong() to MangaStatusPresentation(MR.strings.ongoing, Icons.Outlined.Schedule),
    SManga.COMPLETED.toLong() to MangaStatusPresentation(MR.strings.completed, Icons.Outlined.DoneAll),
    SManga.LICENSED.toLong() to MangaStatusPresentation(MR.strings.licensed, Icons.Outlined.AttachMoney),
    SManga.PUBLISHING_FINISHED.toLong() to MangaStatusPresentation(MR.strings.publishing_finished, Icons.Outlined.Done),
    SManga.CANCELLED.toLong() to MangaStatusPresentation(MR.strings.cancelled, Icons.Outlined.Close),
    SManga.ON_HIATUS.toLong() to MangaStatusPresentation(MR.strings.on_hiatus, Icons.Outlined.Pause),
)

private val unknownStatus = MangaStatusPresentation(MR.strings.unknown, Icons.Outlined.Block)

internal fun mangaStatusPresentation(status: Long): MangaStatusPresentation =
    statusPresentations[status] ?: unknownStatus
