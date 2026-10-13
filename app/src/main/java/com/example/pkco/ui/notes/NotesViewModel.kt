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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The screens of the app; switched here instead of with a navigation library. */
enum class Screen { List, Create }

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {
    /** Notes from the repository, most recently edited first; null until the first load. */
    val notes: StateFlow<List<NoteWithDetails>?> = repository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _screen = MutableStateFlow(Screen.List)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun openCreate() {
        _screen.value = Screen.Create
    }

    fun showList() {
        _screen.value = Screen.List
    }

    /** Saves a new note with trimmed text, no category and no tags, then returns to the list. */
    fun save(title: String, body: String) {
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                NotesViewModel(RoomNoteRepository(PkcoDatabase.getInstance(application)))
            }
        }
    }
}
