package com.example.pkco.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pkco.R

/** Shows the list, create, view or edit screen, as chosen by [NotesViewModel.screen]. */
@Composable
fun NotesApp(viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory)) {
    when (val screen = viewModel.screen.collectAsStateWithLifecycle().value) {
        Screen.List -> {
            val notes by viewModel.notes.collectAsStateWithLifecycle()
            NoteListScreen(notes = notes, onAddNote = viewModel::openCreate, onOpenNote = viewModel::openNote)
        }
        Screen.Create -> {
            val saving by viewModel.saving.collectAsStateWithLifecycle()
            // Back discards the draft, like Cancel.
            BackHandler(onBack = viewModel::showList)
            NoteFormScreen(
                heading = stringResource(R.string.new_note),
                saving = saving,
                onCancel = viewModel::showList,
                onSave = viewModel::saveNew
            )
        }
        is Screen.View -> {
            BackHandler(onBack = viewModel::showList)
            // Drawn only once the selected note has loaded; a stale note with another id is skipped.
            val note = viewModel.selectedNote.collectAsStateWithLifecycle().value?.note
            if (note != null && note.id == screen.noteId) {
                NoteViewScreen(
                    note = note,
                    onBack = viewModel::showList,
                    onEdit = { viewModel.openEdit(note.id) },
                    onDelete = { viewModel.delete(note.id) }
                )
            }
        }
        is Screen.Edit -> {
            val saving by viewModel.saving.collectAsStateWithLifecycle()
            val backToView = { viewModel.openNote(screen.noteId) }
            // Back discards the changes, like Cancel.
            BackHandler(onBack = backToView)
            val note = viewModel.selectedNote.collectAsStateWithLifecycle().value?.note
            if (note != null && note.id == screen.noteId) {
                key(note.id) {
                    NoteFormScreen(
                        heading = stringResource(R.string.edit_note),
                        saving = saving,
                        onCancel = backToView,
                        onSave = { title, body -> viewModel.saveEdit(note.id, title, body) },
                        initialTitle = note.title,
                        initialBody = note.body
                    )
                }
            }
        }
    }
}
