package com.example.pkco.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.pkco.R

/**
 * Edits the note's free tags: an "Add tag" field with an Add button (the keyboard Done action also adds), and
 * the tags as chips; tapping a chip removes it. Names are trimmed, blanks are ignored, and a name already in
 * [tags] (ignoring case) is not added again.
 */
@Composable
fun TagEditor(tags: List<String>, onTagsChange: (List<String>) -> Unit, modifier: Modifier = Modifier) {
    var input by rememberSaveable { mutableStateOf("") }
    val add = {
        val name = input.trim()
        if (name.isNotEmpty() && tags.none { it.equals(name, ignoreCase = true) }) onTagsChange(tags + name)
        input = ""
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.add_tag)) },
                singleLine = true,
                // The keyboard stays open after Done, so several tags can be typed in a row.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() })
            )
            TextButton(onClick = { add() }) { Text(stringResource(R.string.add)) }
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
                        onClick = { onTagsChange(tags - tag) },
                        label = { Text(stringResource(R.string.tag_chip, tag)) },
                        modifier = Modifier.semantics { contentDescription = removeLabel }
                    )
                }
            }
        }
    }
}
