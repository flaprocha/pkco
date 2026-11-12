package com.example.pkco.ui.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pkco.data.entity.Category
import com.example.pkco.data.entity.Note
import com.example.pkco.data.entity.NoteWithDetails
import com.example.pkco.data.entity.Tag
import com.example.pkco.data.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [NotesViewModel] with a repository whose reads fail until told otherwise: the failure is exposed as
 * [NotesViewModel.readFailed] instead of crashing the app (an uncaught exception in viewModelScope would end the
 * test process), and [NotesViewModel.retryReads] recovers once the reads work again.
 */
@RunWith(AndroidJUnit4::class)
class NotesViewModelReadErrorTest {
    private val note = Note(id = "n1", title = "Title", body = "Body", categoryId = null, createdAt = 1, updatedAt = 1)
    private val repository = FlakyReadRepository(note)
    private val viewModel = NotesViewModel(repository)

    /** Collects the ViewModel's flows the way the screens do, so their reads run. */
    private val collectors = CoroutineScope(Dispatchers.Main.immediate)

    private fun onMain(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)

    private fun collect(vararg flows: Flow<*>) {
        flows.forEach { flow -> collectors.launch { flow.collect {} } }
    }

    private fun <T> await(block: suspend () -> T): T = runBlocking { withTimeout(5_000) { block() } }

    @After
    fun tearDown() {
        collectors.cancel()
    }

    @Test
    fun listReadsFail_exposeErrorAndRecoverAfterRetry() {
        collect(viewModel.notes, viewModel.searchResults, viewModel.categories, viewModel.filterTags)

        await { viewModel.readFailed.first { it } }

        repository.failing = false
        onMain { viewModel.retryReads() }

        assertEquals(listOf(note.id), await { viewModel.notes.first { it != null } }!!.map { it.note.id })
        assertEquals(listOf(note.id), await { viewModel.searchResults.first { it != null } }!!.map { it.note.id })
        assertEquals(emptyList<Category>(), await { viewModel.categories.first { it != null } })
        assertFalse(viewModel.readFailed.value)
    }

    @Test
    fun viewedNoteReadFails_exposeErrorKeepScreenAndRecoverAfterRetry() {
        onMain { viewModel.openNote(note.id) }
        collect(viewModel.selectedNote)

        await { viewModel.readFailed.first { it } }
        assertEquals(Screen.View(note.id), viewModel.screen.value)

        repository.failing = false
        onMain { viewModel.retryReads() }

        assertEquals(note.id, await { viewModel.selectedNote.first { it != null } }!!.note.id)
        assertFalse(viewModel.readFailed.value)
        assertEquals(Screen.View(note.id), viewModel.screen.value)
    }

    /** Every read throws while [failing] is true, as a database that cannot be opened would; writes are unused. */
    private class FlakyReadRepository(private val note: Note) : NoteRepository {
        @Volatile var failing = true

        private fun <T> read(value: () -> T): Flow<T> = flow {
            if (failing) throw IllegalStateException("read failed")
            emit(value())
        }

        private fun details() = NoteWithDetails(note, category = null, tags = emptyList())

        override fun observeNotes(): Flow<List<NoteWithDetails>> = read { listOf(details()) }
        override fun searchNotes(query: String, categoryId: String?, tagId: String?) = observeNotes()
        override fun observeNote(id: String): Flow<NoteWithDetails?> = read { details().takeIf { id == note.id } }
        override fun observeCategories(): Flow<List<Category>> = read { emptyList() }
        override fun observeTags(): Flow<List<Tag>> = read { emptyList() }
        override suspend fun createNote(title: String, body: String, categoryId: String?, tagNames: List<String>) =
            error("unused")
        override suspend fun updateNote(
            id: String,
            title: String,
            body: String,
            categoryId: String?,
            tagNames: List<String>,
        ) = error("unused")
        override suspend fun deleteNote(id: String) = error("unused")
        override suspend fun createCategory(name: String): String = error("unused")
        override suspend fun renameCategory(id: String, name: String) = error("unused")
        override suspend fun deleteCategory(id: String) = error("unused")
    }
}
