package nl.potentplum.mise.feature.settings

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.security.ClaudeKeyStatus

/**
 * Claude-sleutel (spec §9 Entry UX): a password field without suggestions or IME learning, a soft format check, the live
 * check with `GET /v1/models`, "Toch opslaan" when Claude can't be reached, and "Klembord wissen" after saving. The
 * window is `FLAG_SECURE` while this screen shows.
 */
@Composable
fun KeyScreen(
    onBack: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: KeyViewModel = hiltViewModel(),
) {
    SecureWindow()
    val keyState by viewModel.keyState.collectAsStateWithLifecycle()
    val step by viewModel.step.collectAsStateWithLifecycle()
    // Plain state on purpose: rememberSaveable would write the key into the saved-instance bundle.
    var key by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbar = LocalMiseSnackbar.current
    val clipboardCleared = stringResource(R.string.key_clipboard_cleared)

    Scaffold(topBar = {
        MiseTopAppBar(title = stringResource(R.string.key_title), onOverflowItem = onOverflowItem, onBack = onBack)
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.key_intro), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.key_storage), style = MaterialTheme.typography.bodyMedium, color = MiseTheme.colors.inkSoft)
            Text(stringResource(R.string.key_where), style = MaterialTheme.typography.bodyMedium, color = MiseTheme.colors.inkSoft)
            keyState.masked?.let {
                Text(stringResource(R.string.key_current, it), style = MaterialTheme.typography.titleSmall)
            }
            if (keyState.status == ClaudeKeyStatus.NEEDS_REENTRY) {
                Text(stringResource(R.string.settings_key_reentry_body), color = MaterialTheme.colorScheme.error)
            }

            NoLearningTextInput {
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it
                        viewModel.edited()
                    },
                    label = { Text(stringResource(R.string.key_field_label)) },
                    singleLine = true,
                    enabled = step != KeyEntryStep.CHECKING,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = if (visible) KeyboardType.Uri else KeyboardType.Password,
                            autoCorrectEnabled = false,
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.check(key) }),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = stringResource(if (visible) R.string.key_hide else R.string.key_show),
                            )
                        }
                    },
                    supportingText =
                        if (key.isNotBlank() && !KeyViewModel.looksLikeKey(key)) {
                            { Text(stringResource(R.string.key_format_warning)) }
                        } else {
                            null
                        },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when (step) {
                KeyEntryStep.CHECKING -> {
                    Text(stringResource(R.string.key_checking), style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    TextButton(onClick = viewModel::cancel) { Text(stringResource(R.string.common_cancel)) }
                }

                KeyEntryStep.INVALID -> {
                    Text(stringResource(R.string.key_invalid), color = MaterialTheme.colorScheme.error)
                }

                KeyEntryStep.UNREACHABLE -> {
                    Text(stringResource(R.string.key_unreachable), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.check(key) }) { Text(stringResource(R.string.common_retry)) }
                        Button(onClick = { viewModel.saveAnyway(key) }) { Text(stringResource(R.string.key_save_anyway)) }
                    }
                }

                KeyEntryStep.SAVED -> {
                    Text(
                        stringResource(R.string.key_saved),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (context.clipboard()?.hasPrimaryClip() == true) {
                        OutlinedButton(onClick = {
                            context.clipboard()?.clearPrimaryClip()
                            snackbar.show(clipboardCleared)
                        }) { Text(stringResource(R.string.key_clear_clipboard)) }
                    }
                    Button(onClick = onBack) { Text(stringResource(R.string.common_done)) }
                }

                KeyEntryStep.IDLE -> {
                    Unit
                }
            }

            if (step == KeyEntryStep.IDLE || step == KeyEntryStep.INVALID) {
                Button(onClick = { viewModel.check(key) }, enabled = key.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.key_check_btn))
                }
            }
            Text(stringResource(R.string.key_privacy), style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
        }
    }
}

private fun Context.clipboard(): ClipboardManager? = getSystemService(ClipboardManager::class.java)

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/** No screenshots or recents thumbnail while the key screen shows (spec §9). */
@Composable
private fun SecureWindow() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/** Asks the keyboard not to learn from what is typed in [content] (`IME_FLAG_NO_PERSONALIZED_LEARNING`, spec §9). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NoLearningTextInput(content: @Composable () -> Unit) {
    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->
            val modified =
                object : PlatformTextInputMethodRequest by request {
                    override fun createInputConnection(outAttributes: EditorInfo) =
                        request.createInputConnection(outAttributes).also {
                            outAttributes.imeOptions = outAttributes.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                        }
                }
            nextHandler.startInputMethod(modified)
        },
        content = content,
    )
}
