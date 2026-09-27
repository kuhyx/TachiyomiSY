package eu.kanade.presentation.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.extension.model.Extension
import tachiyomi.i18n.MR

// The action buttons an idle extension row offers, in display order.
internal data class IdleAction(val icon: ImageVector, val label: StringResource, val onClick: () -> Unit)

// Plain so the exhaustive `when` stays out of Compose.
internal fun idleActions(
    extension: Extension,
    onClickItemAction: (Extension) -> Unit,
    onClickItemSecondaryAction: (Extension) -> Unit,
): List<IdleAction> {
    val action = { onClickItemAction(extension) }
    val secondary = { onClickItemSecondaryAction(extension) }
    return when (extension) {
        is Extension.Installed -> {
            listOfNotNull(
                IdleAction(Icons.Outlined.Settings, MR.strings.action_settings, secondary),
                IdleAction(Icons.Outlined.GetApp, MR.strings.ext_update, action).takeIf { extension.hasUpdate },
            )
        }
        is Extension.Untrusted -> {
            listOf(IdleAction(Icons.Outlined.VerifiedUser, MR.strings.ext_trust, action))
        }
        is Extension.Available -> {
            listOfNotNull(
                IdleAction(Icons.Outlined.Public, MR.strings.action_open_in_web_view, secondary)
                    .takeIf { extension.sources.isNotEmpty() },
                IdleAction(Icons.Outlined.GetApp, MR.strings.ext_install, action),
            )
        }
    }
}
