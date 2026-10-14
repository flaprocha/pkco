package com.example.pkco.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pkco.R

/**
 * Edits the note's free tags: an "Add tag" field with an Add button (the keyboard Done action also adds), and
 * the tags as chips; tapping a chip removes it. Names are trimmed, blanks are ignored, and a name already in
 * [tags] (ignoring case) is not added again. A name is at most [InputLimits.TAG] characters (cut off while typing);
 * with [InputLimits.TAGS_PER_NOTE] tags, Add only shows "Maximum 20 tags".
 */
@Composable
fun TagEditor(tags: List<String>, onTagsChange: (List<String>) -> Unit, modifier: Modifier = Modifier) {
    var input by rememberSaveable { mutableStateOf("") }
    var tooMany by rememberSaveable { mutableStateOf(false) }
    val add = {
        val name = input.trim()
        when {
            name.isEmpty() || tags.any { it.equals(name, ignoreCase = true) } -> input = ""
            tags.size >= InputLimits.TAGS_PER_NOTE -> tooMany = true
            else -> {
                onTagsChange(tags + name)
                input = ""
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Top-aligned, so the Add button stays beside the field when the "Maximum" message shows under it.
        Row(verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it.take(InputLimits.TAG)
                    tooMany = false
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag(FormTestTags.TAG),
                placeholder = { Text(stringResource(R.string.add_tag)) },
                isError = tooMany,
                supportingText = if (tooMany) {
                    { Text(stringResource(R.string.max_tags, InputLimits.TAGS_PER_NOTE)) }
                } else {
                    null
                },
                singleLine = true,
                // The keyboard stays open after Done, so several tags can be typed in a row.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() })
            )
            TextButton(onClick = { add() }, modifier = Modifier.padding(top = 4.dp)) { Text(stringResource(R.string.add)) }
        }
        if (tags.isNotEmpty()) {
            // Capped in height, so many tags scroll here instead of squeezing the body field.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 104.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val removeLabel = stringResource(R.string.remove_tag, tag)
                    InputChip(
                        selected = false,
                        onClick = {
                            onTagsChange(tags - tag)
                            tooMany = false
                        },
                        label = {
                            Text(stringResource(R.string.tag_chip, tag), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        modifier = Modifier.semantics { contentDescription = removeLabel }
                    )
                }
            }
        }
    }
}
