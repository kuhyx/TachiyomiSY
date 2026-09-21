package eu.kanade.presentation.more.settings.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.api.get

internal data class LoginDialog(
    val tracker: Tracker,
    val uNameStringRes: StringResource,
)

internal data class LogoutDialog(
    val tracker: Tracker,
)

@Composable
internal fun TrackingLoginDialog(
    tracker: Tracker,
    uNameStringRes: StringResource,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(TextFieldValue(tracker.getUsername())) }
    val password = rememberTextFieldState(tracker.getPassword())
    var processing by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { LoginTitle(tracker.name, onDismissRequest) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Username + ContentType.EmailAddress },
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(text = stringResource(uNameStringRes)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = inputError && !processing,
                )
                PasswordField(password, isError = inputError && !processing)
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !processing && username.text.isNotBlank() && password.text.isNotBlank(),
                onClick = {
                    scope.launchIO {
                        processing = true
                        val result = checkLogin(
                            context = context,
                            tracker = tracker,
                            username = username.text,
                            password = password.text.toString(),
                        )
                        inputError = !result
                        if (result) onDismissRequest()
                        processing = false
                    }
                },
            ) {
                val id = if (processing) MR.strings.logging_in else MR.strings.login
                Text(text = stringResource(id))
            }
        },
    )
}

@Composable
internal fun LoginTitle(trackerName: String, onDismissRequest: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(MR.strings.login_title, trackerName),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismissRequest) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(MR.strings.action_close),
            )
        }
    }
}

@Composable
internal fun PasswordField(password: TextFieldState, isError: Boolean) {
    var hidePassword by remember { mutableStateOf(true) }
    OutlinedSecureTextField(
        state = password,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.Password },
        label = { Text(text = stringResource(MR.strings.password)) },
        trailingIcon = {
            IconButton(onClick = { hidePassword = !hidePassword }) {
                Icon(
                    imageVector = if (hidePassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = null,
                )
            }
        },
        textObfuscationMode = if (hidePassword) TextObfuscationMode.Hidden else TextObfuscationMode.Visible,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        isError = isError,
    )
}

@Composable
internal fun TrackingLogoutDialog(
    tracker: Tracker,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(MR.strings.logout_title, tracker.name),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismissRequest,
                ) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tracker.logout()
                        onDismissRequest()
                        context.toast(MR.strings.logout_success)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(text = stringResource(MR.strings.logout))
                }
            }
        },
    )
}

private suspend fun checkLogin(
    context: Context,
    tracker: Tracker,
    username: String,
    password: String,
): Boolean {
    return try {
        tracker.login(username, password)
        withUIContext { context.toast(MR.strings.login_success) }
        true
    } catch (expected: Throwable) {
        // Any failure ends here and the fallback below applies.
        tracker.logout()
        withUIContext { context.toast(expected.message.toString()) }
        false
    }
}
