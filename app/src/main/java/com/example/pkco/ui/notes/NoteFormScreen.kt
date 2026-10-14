package com.example.pkco.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.pkco.R
import com.example.pkco.data.entity.Category

/** Saves the tag list as an ArrayList, which a Bundle can hold even when it is empty. */
private val TagListSaver = Saver<List<String>, ArrayList<String>>(save = { ArrayList(it) }, restore = { it })

/**
 * The form to create or edit a note, titled [heading]: title, category, tags and body. The fields start with
 * the initial values. The draft lives only while this screen is shown; leaving it discards the draft. A
 * category created from the dropdown is saved at once and stays even if the draft is discarded. [categories] is
 * null while loading; a selected category that is deleted meanwhile falls back to "No category". Cancel is
 * disabled while [saving].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFormScreen(
    heading: String,
    saving: Boolean,
    categories: List<Category>?,
    onCreateCategory: suspend (name: String) -> String?,
    onCancel: () -> Unit,
    onSave: (title: String, body: String, categoryId: String?, tagNames: List<String>) -> Unit,
    initialTitle: String = "",
    initialBody: String = "",
    initialCategoryId: String? = null,
    initialTags: List<String> = emptyList(),
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var body by rememberSaveable { mutableStateOf(initialBody) }
    var categoryId by rememberSaveable { mutableStateOf(initialCategoryId) }
    var tags by rememberSaveable(stateSaver = TagListSaver) { mutableStateOf(initialTags) }
    val canSave = !saving && (title.isNotBlank() || body.isNotBlank())

    // Keyed on the list only: a category just created from the dropdown is selected before the list has it.
    LaunchedEffect(categories) {
        if (categories != null && categoryId != null && categories.none { it.id == categoryId }) categoryId = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(heading) },
                navigationIcon = {
                    TextButton(onClick = onCancel, enabled = !saving) { Text(stringResource(R.string.cancel)) }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(title, body, categoryId, tags) },
                        enabled = canSave,
                        modifier = Modifier.testTag(FormTestTags.SAVE)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                // Shrinks the column above the keyboard; the body field then scrolls inside itself.
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(InputLimits.TITLE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FormTestTags.TITLE),
                placeholder = { Text(stringResource(R.string.title_hint)) },
                supportingText = { Text(stringResource(R.string.char_count, title.length, InputLimits.TITLE)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )
            CategoryDropdown(
                categories = categories.orEmpty(),
                selectedId = categoryId,
                onSelect = { categoryId = it },
                onCreateCategory = onCreateCategory,
                modifier = Modifier.fillMaxWidth()
            )
            TagEditor(tags = tags, onTagsChange = { tags = it }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = body,
                onValueChange = { body = it.take(InputLimits.BODY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag(FormTestTags.BODY),
                placeholder = { Text(stringResource(R.string.body_hint)) },
                supportingText = if (body.length > InputLimits.BODY_COUNTER_FROM) {
                    { Text(stringResource(R.string.char_count, body.length, InputLimits.BODY)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        }
    }
}

/** Test tags of the form's fields, used by the UI test. */
internal object FormTestTags {
    const val TITLE = "note_title"
    const val BODY = "note_body"
    const val TAG = "note_tag"
    const val SAVE = "note_save"
}
