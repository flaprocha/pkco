package com.example.pkco.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pkco.data.PkcoDatabase
import com.example.pkco.data.entity.Category
import com.example.pkco.data.entity.NoteWithDetails
import com.example.pkco.data.repository.NoteRepository
import com.example.pkco.data.repository.RoomNoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The screens of the app; switched here instead of with a navigation library. */
sealed interface Screen {
    data object List : Screen
    data object Create : Screen
    data class View(val noteId: String) : Screen
    data class Edit(val noteId: String) : Screen
    data object Categories : Screen
}

/** The note id shown by a View or Edit screen, or null for the other screens. */
private val Screen.noteId: String?
    get() = when (this) {
        is Screen.View -> noteId
        is Screen.Edit -> noteId
        Screen.List, Screen.Create, Screen.Categories -> null
    }

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {
    /** Notes from the repository, most recently edited first; null until the first load. */
    val notes: StateFlow<List<NoteWithDetails>?> = repository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** All categories by name; null until the first load. */
    val categories: StateFlow<List<Category>?> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _screen = MutableStateFlow<Screen>(Screen.List)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    /**
     * The note of the View or Edit screen, null while it loads. It can briefly hold the previously selected
     * note, so callers check its id. When the note no longer exists, the app returns to the list.
     */
    val selectedNote: StateFlow<NoteWithDetails?> = _screen
        .map { it.noteId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                repository.observeNote(id).onEach { if (it == null && _screen.value.noteId == id) showList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun openCreate() {
        _screen.value = Screen.Create
    }

    fun showList() {
        _screen.value = Screen.List
    }

    fun openNote(id: String) {
        _screen.value = Screen.View(id)
    }

    fun openEdit(id: String) {
        _screen.value = Screen.Edit(id)
    }

    fun openCategories() {
        _screen.value = Screen.Categories
    }

    /** Saves a new note with trimmed text, the chosen category and tags, then returns to the list. */
    fun saveNew(title: String, body: String, categoryId: String?, tagNames: List<String>) {
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()
        if (_saving.value || (trimmedTitle.isEmpty() && trimmedBody.isEmpty())) return
        _saving.value = true
        viewModelScope.launch {
            try {
                repository.createNote(trimmedTitle, trimmedBody, existingCategoryId(categoryId), tagNames)
                _screen.value = Screen.List
            } finally {
                _saving.value = false
            }
        }
    }

    /**
     * Saves trimmed text, the chosen category and tags to note [id], then returns to its View screen.
     * Nothing is written when text, category and tag set (ignoring case) are all unchanged.
     */
    fun saveEdit(id: String, title: String, body: String, categoryId: String?, tagNames: List<String>) {
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()
        if (_saving.value || (trimmedTitle.isEmpty() && trimmedBody.isEmpty())) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val current = repository.observeNote(id).first()
                if (current == null) {
                    _screen.value = Screen.List
                    return@launch
                }
                val newCategoryId = existingCategoryId(categoryId)
                val changed = trimmedTitle != current.note.title ||
                    trimmedBody != current.note.body ||
                    newCategoryId != current.note.categoryId ||
                    tagKeys(tagNames) != tagKeys(current.tags.map { it.name })
                if (changed) {
                    repository.updateNote(id, trimmedTitle, trimmedBody, newCategoryId, tagNames)
                }
                _screen.value = Screen.View(id)
            } finally {
                _saving.value = false
            }
        }
    }

    /** Returns [categoryId] if that category still exists, otherwise null, so a save never points to a deleted one. */
    private suspend fun existingCategoryId(categoryId: String?): String? =
        categoryId?.takeIf { id -> repository.observeCategories().first().any { it.id == id } }

    /** Tag names as the repository compares them: trimmed, non-blank, ignoring case. */
    private fun tagKeys(names: List<String>): Set<String> =
        names.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    /** Returns the new category's id. Throws [IllegalArgumentException] if the name is blank or taken. */
    suspend fun createCategory(name: String): String = repository.createCategory(name)

    /** Throws [IllegalArgumentException] if the name is blank or taken by another category. */
    suspend fun renameCategory(id: String, name: String) = repository.renameCategory(id, name)

    /** Deletes the category; its notes stay and become uncategorized. */
    fun deleteCategory(id: String) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }

    /** Soft-deletes note [id] and returns to the list. */
    fun delete(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
            _screen.value = Screen.List
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                NotesViewModel(RoomNoteRepository(PkcoDatabase.getInstance(application)))
            }
        }
    }
}
