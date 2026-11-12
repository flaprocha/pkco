package com.example.pkco.ui.notes

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pkco.R

/**
 * Shows the list, create, view, edit or categories screen, as chosen by [NotesViewModel.screen], and a Snackbar
 * over any of them when a write fails. When a read fails, a full-screen message with "Try again" replaces them.
 */
@Composable
fun NotesApp(viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory)) {
    // Outside the screen switch, so the list keeps its scroll position while a note is open; saved across rotation.
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val error by viewModel.error.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    LaunchedEffect(error) {
        val failure = error ?: return@LaunchedEffect
        // Marked as shown first, so a rotation while the Snackbar is up does not show it again.
        viewModel.errorShown()
        snackbarHostState.showSnackbar(resources.getString(failure.messageRes()))
    }

    val readFailed by viewModel.readFailed.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // A failed read replaces every screen; the chosen screen comes back after "Try again".
        if (readFailed) ReadErrorScreen(onRetry = viewModel::retryReads) else CurrentScreen(viewModel, listState)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
        )
    }
}

@StringRes
private fun UserError.messageRes(): Int = when (this) {
    UserError.SaveNote -> R.string.error_save_note
    UserError.Delete -> R.string.error_delete
    UserError.SaveCategory -> R.string.error_save_category
}

@Composable
private fun CurrentScreen(viewModel: NotesViewModel, listState: LazyListState) {
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
                onOpenCategories = viewModel::openCategories,
                listState = listState
            )
        }
        Screen.Create -> {
            val saving by viewModel.saving.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            // Back discards the draft, like Cancel; ignored while a save runs, so a failed save keeps the draft.
            BackHandler { if (!saving) viewModel.showList() }
            NoteFormScreen(
                heading = stringResource(R.string.new_note),
                saving = saving,
                categories = categories,
                onCreateCategory = viewModel::createCategory,
                onCancel = viewModel::showList,
                onSave = viewModel::saveNew
            )
        }
        is Screen.View -> {
            val deleting by viewModel.saving.collectAsStateWithLifecycle()
            BackHandler(onBack = viewModel::showList)
            // Drawn once the selected note has loaded; a stale note with another id is skipped. A note that
            // disappears sends the app back to the list (see NotesViewModel.selectedNote).
            val details = viewModel.selectedNote.collectAsStateWithLifecycle().value
            if (details != null && details.note.id == screen.noteId) {
                NoteViewScreen(
                    details = details,
                    deleting = deleting,
                    onBack = viewModel::showList,
                    onEdit = { viewModel.openEdit(screen.noteId) },
                    onDelete = { viewModel.delete(screen.noteId) }
                )
            } else {
                CenteredProgress()
            }
        }
        is Screen.Edit -> {
            val saving by viewModel.saving.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            val backToView = { viewModel.openNote(screen.noteId) }
            // Back discards the changes, like Cancel; ignored while a save runs.
            BackHandler { if (!saving) backToView() }
            val details = viewModel.selectedNote.collectAsStateWithLifecycle().value
            if (details != null && details.note.id == screen.noteId) {
                val note = details.note
                key(note.id) {
                    NoteFormScreen(
                        heading = stringResource(R.string.edit_note),
                        saving = saving,
                        categories = categories,
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
            } else {
                CenteredProgress()
            }
        }
        Screen.Categories -> {
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            BackHandler(onBack = viewModel::showList)
            CategoriesScreen(
                categories = categories,
                onBack = viewModel::showList,
                onCreate = { name -> viewModel.createCategory(name) != null },
                onRename = viewModel::renameCategory,
                onDelete = viewModel::deleteCategory
            )
        }
    }
}
