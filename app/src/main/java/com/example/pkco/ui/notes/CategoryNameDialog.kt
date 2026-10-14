package com.example.pkco.ui.notes

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.pkco.R
import kotlinx.coroutines.launch

/**
 * A dialog with one category name field (at most [InputLimits.CATEGORY_NAME] characters), used to create and to
 * rename a category. [onConfirm] saves the name through the repository and returns false when the write failed
 * (the app shows that message); when it fails or rejects the name, the dialog stays open with the typed name.
 * [onDone] closes it.
 */
@Composable
fun CategoryNameDialog(
    title: String,
    confirmLabel: String,
    onConfirm: suspend (name: String) -> Boolean,
    onDone: () -> Unit,
    initialName: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val confirm = {
        if (!busy) {
            busy = true
            scope.launch {
                val result = saveCategoryName(name, onConfirm)
                busy = false
                when (result) {
                    NameSaveResult.Saved -> onDone()
                    is NameSaveResult.Rejected -> error = result.message
                    NameSaveResult.Failed -> Unit
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(InputLimits.CATEGORY_NAME)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.category_name)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() })
            )
        },
        confirmButton = {
            TextButton(onClick = { confirm() }, enabled = !busy) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** The outcome of saving a category name. */
internal sealed interface NameSaveResult {
    data object Saved : NameSaveResult

    /** The write failed; the ViewModel has already reported it. */
    data object Failed : NameSaveResult

    /** The repository rejected the name; [message] says why. */
    data class Rejected(@param:StringRes val message: Int) : NameSaveResult
}

/**
 * Runs [save] with [name], which returns false when the write failed. A name the repository rejects with
 * [IllegalArgumentException] (blank, or already used ignoring case) gives [NameSaveResult.Rejected].
 */
internal suspend fun saveCategoryName(name: String, save: suspend (String) -> Boolean): NameSaveResult =
    try {
        if (save(name)) NameSaveResult.Saved else NameSaveResult.Failed
    } catch (_: IllegalArgumentException) {
        NameSaveResult.Rejected(if (name.isBlank()) R.string.category_name_blank else R.string.category_name_taken)
    }
