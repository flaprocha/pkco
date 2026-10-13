package com.example.pkco.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Shows the list or the create screen, as chosen by [NotesViewModel.screen]. */
@Composable
fun NotesApp(viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory)) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    when (screen) {
        Screen.List -> {
            val notes by viewModel.notes.collectAsStateWithLifecycle()
            NoteListScreen(notes = notes, onAddNote = viewModel::openCreate)
        }
        Screen.Create -> {
            val saving by viewModel.saving.collectAsStateWithLifecycle()
            // Back discards the draft, like Cancel.
            BackHandler(onBack = viewModel::showList)
            CreateNoteScreen(saving = saving, onCancel = viewModel::showList, onSave = viewModel::save)
        }
    }
}
