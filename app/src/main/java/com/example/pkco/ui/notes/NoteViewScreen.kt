package com.example.pkco.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.pkco.R
import com.example.pkco.data.entity.NoteWithDetails
import java.time.Instant

/**
 * One note in full with its category and tags, with Edit and Delete. Delete asks for confirmation first.
 * Links in the body stay plain text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteViewScreen(details: NoteWithDetails, onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val note = details.note
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                },
                actions = {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                    TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete)) }
                }
            )
        }
    ) { innerPadding ->
        val formatter = rememberEditedAtFormatter()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (note.title.isBlank()) {
                Text(
                    stringResource(R.string.untitled),
                    style = MaterialTheme.typography.headlineSmall,
                    fontStyle = FontStyle.Italic
                )
            } else {
                SelectionContainer {
                    Text(note.title, style = MaterialTheme.typography.headlineSmall)
                }
            }
            details.category?.let {
                Text(
                    stringResource(R.string.note_category, it.name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            val tags = tagsText(details.tags)
            if (tags.isNotEmpty()) {
                Text(
                    stringResource(R.string.note_tags, tags),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                stringResource(R.string.edited_at, formatter.format(Instant.ofEpochMilli(note.updatedAt))),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (note.body.isNotBlank()) {
                SelectionContainer {
                    Text(note.body, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
