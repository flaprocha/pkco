package com.example.pkco.ui.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pkco.data.entity.Category
import com.example.pkco.data.entity.Note
import com.example.pkco.data.entity.NoteWithDetails
import com.example.pkco.data.entity.Tag
import com.example.pkco.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** [NotesViewModel] with a repository whose writes all fail: the error is reported and the screen stays. */
@RunWith(AndroidJUnit4::class)
class NotesViewModelErrorTest {
    private val note = Note(id = "n1", title = "Title", body = "Body", categoryId = null, createdAt = 1, updatedAt = 1)

    /** Runs [action] on the main thread, where the app calls the ViewModel. */
    private fun onMain(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)

    /** Waits until the write started by the last call has finished and reported its error. */
    private fun NotesViewModel.awaitError(): UserError? = runBlocking {
        withTimeout(5_000) {
            val failure = error.first { it != null }
            saving.first { !it }
            failure
        }
    }

    @Test
    fun saveNew_failure_keepsCreateScreenAndResetsSaving() {
        val viewModel = NotesViewModel(FailingRepository(note))
        onMain {
            viewModel.openCreate()
            viewModel.saveNew("Title", "Body", null, listOf("tag"))
        }

        assertEquals(UserError.SaveNote, viewModel.awaitError())
        assertEquals(Screen.Create, viewModel.screen.value)
        assertFalse(viewModel.saving.value)

        onMain { viewModel.errorShown() }
        assertNull(viewModel.error.value)
    }

    @Test
    fun saveEdit_failure_keepsEditScreenAndResetsSaving() {
        val viewModel = NotesViewModel(FailingRepository(note))
        onMain {
            viewModel.openEdit(note.id)
            viewModel.saveEdit(note.id, "Changed", "Body", null, emptyList())
        }

        assertEquals(UserError.SaveNote, viewModel.awaitError())
        assertEquals(Screen.Edit(note.id), viewModel.screen.value)
        assertFalse(viewModel.saving.value)
    }

    @Test
    fun delete_failure_keepsViewScreen() {
        val viewModel = NotesViewModel(FailingRepository(note))
        onMain {
            viewModel.openNote(note.id)
            viewModel.delete(note.id)
        }

        assertEquals(UserError.Delete, viewModel.awaitError())
        assertEquals(Screen.View(note.id), viewModel.screen.value)
    }

    @Test
    fun deleteCategory_failure_setsError() {
        val viewModel = NotesViewModel(FailingRepository(note))
        onMain { viewModel.deleteCategory("c1") }

        assertEquals(UserError.Delete, viewModel.awaitError())
    }

    @Test
    fun createAndRenameCategory_failure_setsErrorButRejectedNameStillThrows() = runBlocking {
        val viewModel = NotesViewModel(FailingRepository(note))

        assertNull(viewModel.createCategory("Work"))
        assertEquals(UserError.SaveCategory, viewModel.error.value)
        viewModel.errorShown()

        assertFalse(viewModel.renameCategory("c1", "Home"))
        assertEquals(UserError.SaveCategory, viewModel.error.value)
        viewModel.errorShown()

        // A blank name is a rejection the screen shows itself, not a failed write.
        assertThrows(IllegalArgumentException::class.java) { runBlocking { viewModel.createCategory(" ") } }
        assertNull(viewModel.error.value)
    }

    /** Reads return [note]; every write throws, as a full disk or a closed database would. */
    private class FailingRepository(private val note: Note) : NoteRepository {
        private fun fail(): Nothing = throw IllegalStateException("write failed")

        override fun observeNotes(): Flow<List<NoteWithDetails>> = flowOf(listOf(details()))
        override fun searchNotes(query: String, categoryId: String?, tagId: String?) = observeNotes()
        override fun observeNote(id: String): Flow<NoteWithDetails?> = flowOf(details().takeIf { id == note.id })
        override suspend fun createNote(title: String, body: String, categoryId: String?, tagNames: List<String>) =
            fail()
        override suspend fun updateNote(
            id: String,
            title: String,
            body: String,
            categoryId: String?,
            tagNames: List<String>,
        ) = fail()
        override suspend fun deleteNote(id: String) = fail()
        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun createCategory(name: String): String {
            require(name.isNotBlank())
            fail()
        }
        override suspend fun renameCategory(id: String, name: String) {
            require(name.isNotBlank())
            fail()
        }
        override suspend fun deleteCategory(id: String) = fail()
        override fun observeTags(): Flow<List<Tag>> = flowOf(emptyList())

        private fun details() = NoteWithDetails(note, category = null, tags = emptyList())
    }
}
