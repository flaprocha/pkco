package com.example.pkco.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pkco.data.PkcoDatabase
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
}

/** The note id shown by a View or Edit screen, or null for the other screens. */
private val Screen.noteId: String?
    get() = when (this) {
        is Screen.View -> noteId
        is Screen.Edit -> noteId
        Screen.List, Screen.Create -> null
    }

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {
    /** Notes from the repository, most recently edited first; null until the first load. */
    val notes: StateFlow<List<NoteWithDetails>?> = repository.observeNotes()
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

    /** Saves a new note with trimmed text, no category and no tags, then returns to the list. */
    fun saveNew(title: String, body: String) {
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()
        if (_saving.value || (trimmedTitle.isEmpty() && trimmedBody.isEmpty())) return
        _saving.value = true
        viewModelScope.launch {
            try {
                repository.createNote(trimmedTitle, trimmedBody, categoryId = null, tagNames = emptyList())
                _screen.value = Screen.List
            } finally {
                _saving.value = false
            }
        }
    }

    /**
     * Saves trimmed text to note [id], then returns to its View screen. Nothing is written when the text is
     * unchanged. The note keeps its current category and tags, since updateNote replaces them.
     */
    fun saveEdit(id: String, title: String, body: String) {
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
                if (trimmedTitle != current.note.title || trimmedBody != current.note.body) {
                    repository.updateNote(
                        id,
                        trimmedTitle,
                        trimmedBody,
                        categoryId = current.note.categoryId,
                        tagNames = current.tags.map { it.name },
                    )
                }
                _screen.value = Screen.View(id)
            } finally {
                _saving.value = false
            }
        }
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
