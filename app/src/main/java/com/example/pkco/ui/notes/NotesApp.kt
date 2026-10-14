package com.example.pkco.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pkco.R

/** Shows the list, create, view, edit or categories screen, as chosen by [NotesViewModel.screen]. */
@Composable
fun NotesApp(viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory)) {
    when (val screen = viewModel.screen.collectAsStateWithLifecycle().value) {
        Screen.List -> {
            val allNotes by viewModel.notes.collectAsStateWithLifecycle()
            val results by viewModel.searchResults.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            val tags by viewModel.filterTags.collectAsStateWithLifecycle()
            val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
            val tagFilter by viewModel.tagFilter.collectAsStateWithLifecycle()
            NoteListScreen(
                hasNotes = allNotes?.isNotEmpty(),
                notes = results,
                searchText = viewModel.searchText,
                onSearchTextChange = viewModel::updateSearchText,
                categories = categories.orEmpty(),
                selectedCategoryId = categoryFilter,
                onSelectCategory = viewModel::selectCategoryFilter,
                tags = tags.orEmpty(),
                selectedTagId = tagFilter,
                onSelectTag = viewModel::selectTagFilter,
                onClearSearch = viewModel::clearSearch,
                onAddNote = viewModel::openCreate,
                onOpenNote = viewModel::openNote,
                onOpenCategories = viewModel::openCategories
            )
        }
        Screen.Create -> {
            val saving by viewModel.saving.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            // Back discards the draft, like Cancel.
            BackHandler(onBack = viewModel::showList)
            NoteFormScreen(
                heading = stringResource(R.string.new_note),
                saving = saving,
                categories = categories.orEmpty(),
                onCreateCategory = viewModel::createCategory,
                onCancel = viewModel::showList,
                onSave = viewModel::saveNew
            )
        }
        is Screen.View -> {
            BackHandler(onBack = viewModel::showList)
            // Drawn only once the selected note has loaded; a stale note with another id is skipped.
            val details = viewModel.selectedNote.collectAsStateWithLifecycle().value
            if (details != null && details.note.id == screen.noteId) {
                NoteViewScreen(
                    details = details,
                    onBack = viewModel::showList,
                    onEdit = { viewModel.openEdit(screen.noteId) },
                    onDelete = { viewModel.delete(screen.noteId) }
                )
            }
        }
        is Screen.Edit -> {
            val saving by viewModel.saving.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            val backToView = { viewModel.openNote(screen.noteId) }
            // Back discards the changes, like Cancel.
            BackHandler(onBack = backToView)
            val details = viewModel.selectedNote.collectAsStateWithLifecycle().value
            if (details != null && details.note.id == screen.noteId) {
                val note = details.note
                key(note.id) {
                    NoteFormScreen(
                        heading = stringResource(R.string.edit_note),
                        saving = saving,
                        categories = categories.orEmpty(),
                        onCreateCategory = viewModel::createCategory,
                        onCancel = backToView,
                        onSave = { title, body, categoryId, tagNames ->
                            viewModel.saveEdit(note.id, title, body, categoryId, tagNames)
                        },
                        initialTitle = note.title,
                        initialBody = note.body,
                        initialCategoryId = note.categoryId,
                        initialTags = sortedTagNames(details.tags)
                    )
                }
            }
        }
        Screen.Categories -> {
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            BackHandler(onBack = viewModel::showList)
            CategoriesScreen(
                categories = categories,
                onBack = viewModel::showList,
                onCreate = { name -> viewModel.createCategory(name) },
                onRename = viewModel::renameCategory,
                onDelete = viewModel::deleteCategory
            )
        }
    }
}
