package com.example.pkco.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pkco.data.PkcoDatabase
import com.example.pkco.data.entity.Category
import com.example.pkco.data.entity.NoteWithDetails
import com.example.pkco.data.entity.Tag
import com.example.pkco.data.repository.NoteRepository
import com.example.pkco.data.repository.RoomNoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

/** What the list is searched and filtered by; null ids mean "All". */
private data class SearchParams(val text: String, val categoryId: String?, val tagId: String?)

/** A write that failed. The screen maps it to a message and shows it once. */
enum class UserError { SaveNote, Delete, SaveCategory }

/** The note id shown by a View or Edit screen, or null for the other screens. */
private val Screen.noteId: String?
    get() = when (this) {
        is Screen.View -> noteId
        is Screen.Edit -> noteId
        Screen.List, Screen.Create, Screen.Categories -> null
    }

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {
    private val _readFailed = MutableStateFlow(false)
    /**
     * True after a repository read (a Flow the screens collect) has failed, for example because the database
     * cannot be opened. The app then shows a full-screen message until [retryReads].
     */
    val readFailed: StateFlow<Boolean> = _readFailed.asStateFlow()

    /** Bumped by [retryReads]; every read restarts when it changes. */
    private val readAttempt = MutableStateFlow(0)

    /**
     * Stops a failed read here instead of letting it crash the app: [readFailed] is set and the read emits nothing
     * more until [retryReads]. Cancellation is passed on. The exception itself is not shown or logged.
     */
    private fun <T> Flow<T>.catchRead(): Flow<T> = catch { e ->
        if (e is CancellationException) throw e
        _readFailed.value = true
    }

    /** Runs [read] again each time [retryReads] is called; failures are caught by [catchRead]. */
    private fun <T> restartable(read: () -> Flow<T>): Flow<T> = readAttempt.flatMapLatest { read().catchRead() }

    /** Clears [readFailed] and restarts every read. */
    fun retryReads() {
        _readFailed.value = false
        readAttempt.value++
    }

    /** Notes from the repository, most recently edited first; null until the first load. */
    val notes: StateFlow<List<NoteWithDetails>?> = restartable { repository.observeNotes() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** All categories by name; null until the first load. */
    val categories: StateFlow<List<Category>?> = restartable { repository.observeCategories() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The list's search text. Compose state, so the search field never lags behind typing. */
    var searchText by mutableStateOf("")
        private set

    private val _categoryFilter = MutableStateFlow<String?>(null)
    /** The category id the list is filtered by, or null for all. */
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    private val _tagFilter = MutableStateFlow<String?>(null)
    /** The tag id the list is filtered by, or null for all. */
    val tagFilter: StateFlow<String?> = _tagFilter.asStateFlow()

    /** Tags used by at least one note that is not deleted, by name ignoring case; null until the first load. */
    val filterTags: StateFlow<List<Tag>?> = notes
        .map { list ->
            list?.flatMap { it.tags }?.distinctBy { it.id }?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The notes shown on the list: [searchText] in relevance order, within the selected filters; null until the
     * first load. A filter whose category was deleted, or whose tag no longer has notes, is reset to "All".
     */
    val searchResults: StateFlow<List<NoteWithDetails>?> = combine(
        snapshotFlow { searchText },
        _categoryFilter,
        _tagFilter,
        categories,
        filterTags,
    ) { text, categoryId, tagId, allCategories, usedTags ->
        // While a list is still loading (null), the selection is kept.
        val validCategoryId = categoryId?.takeIf { id -> allCategories == null || allCategories.any { it.id == id } }
        val validTagId = tagId?.takeIf { id -> usedTags == null || usedTags.any { it.id == id } }
        SearchParams(text, validCategoryId, validTagId)
    }
        .onEach { params ->
            if (params.categoryId != _categoryFilter.value) _categoryFilter.value = params.categoryId
            if (params.tagId != _tagFilter.value) _tagFilter.value = params.tagId
        }
        .distinctUntilChanged()
        // Also emits the same parameters again after retryReads, which restarts the search.
        .combine(readAttempt) { params, _ -> params }
        .flatMapLatest { repository.searchNotes(it.text, it.categoryId, it.tagId).catchRead() }
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
        .combine(readAttempt) { id, _ -> id }
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                repository.observeNote(id)
                    .onEach { if (it == null && _screen.value.noteId == id) showList() }
                    .catchRead()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _saving = MutableStateFlow(false)
    /** True while a note is being saved or deleted; a second save or delete is ignored meanwhile. */
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<UserError?>(null)
    /** The last failed write, until the screen calls [errorShown]. */
    val error: StateFlow<UserError?> = _error.asStateFlow()

    fun errorShown() {
        _error.value = null
    }

    fun updateSearchText(text: String) {
        searchText = text
    }

    fun selectCategoryFilter(categoryId: String?) {
        _categoryFilter.value = categoryId
    }

    fun selectTagFilter(tagId: String?) {
        _tagFilter.value = tagId
    }

    /** Empties the search text and sets both filters back to "All". */
    fun clearSearch() {
        searchText = ""
        _categoryFilter.value = null
        _tagFilter.value = null
    }

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

    /**
     * Saves a new note with trimmed text, the chosen category and tags, then returns to the list. On failure the
     * form stays open with its text and [error] is set.
     */
    fun saveNew(title: String, body: String, categoryId: String?, tagNames: List<String>) {
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()
        if (_saving.value || (trimmedTitle.isEmpty() && trimmedBody.isEmpty())) return
        _saving.value = true
        viewModelScope.launch {
            try {
                repository.createNote(trimmedTitle, trimmedBody, existingCategoryId(categoryId), tagNames)
                if (_screen.value == Screen.Create) _screen.value = Screen.List
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _error.value = UserError.SaveNote
            } finally {
                _saving.value = false
            }
        }
    }

    /**
     * Saves trimmed text, the chosen category and tags to note [id], then returns to its View screen.
     * Nothing is written when text, category and tag set (ignoring case) are all unchanged. On failure the form
     * stays open with its text and [error] is set.
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
                    // Deleted meanwhile; selectedNote also returns to the list.
                    if (_screen.value == Screen.Edit(id)) _screen.value = Screen.List
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
                if (_screen.value == Screen.Edit(id)) _screen.value = Screen.View(id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _error.value = UserError.SaveNote
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

    /**
     * Returns the new category's id, or null when the write failed ([error] is then set). Throws
     * [IllegalArgumentException] if the name is blank or taken.
     */
    suspend fun createCategory(name: String): String? =
        reportFailure(UserError.SaveCategory, passRejectedName = true) {
            repository.createCategory(name)
        }

    /**
     * Returns false when the write failed ([error] is then set). Throws [IllegalArgumentException] if the name is
     * blank or taken by another category.
     */
    suspend fun renameCategory(id: String, name: String): Boolean =
        reportFailure(UserError.SaveCategory, passRejectedName = true) {
            repository.renameCategory(id, name)
        } != null

    /** Deletes the category; its notes stay and become uncategorized. */
    fun deleteCategory(id: String) {
        viewModelScope.launch { reportFailure(UserError.Delete) { repository.deleteCategory(id) } }
    }

    /** Soft-deletes note [id] and returns to the list. On failure the View screen stays and [error] is set. */
    fun delete(id: String) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                if (reportFailure(UserError.Delete) { repository.deleteNote(id) } != null &&
                    _screen.value == Screen.View(id)
                ) {
                    _screen.value = Screen.List
                }
            } finally {
                _saving.value = false
            }
        }
    }

    /**
     * Runs [write] and returns its result, or null after setting [error] to [failure] when it throws. Cancellation,
     * and with [passRejectedName] a rejected name ([IllegalArgumentException]), are passed on to the caller.
     */
    private suspend fun <T> reportFailure(
        failure: UserError,
        passRejectedName: Boolean = false,
        write: suspend () -> T,
    ): T? =
        try {
            write()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            if (passRejectedName) throw e
            _error.value = failure
            null
        } catch (_: Exception) {
            _error.value = failure
            null
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
