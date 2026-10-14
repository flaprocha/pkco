package com.example.pkco.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pkco.data.entity.NoteWithDetails
import com.example.pkco.data.repository.RoomNoteRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomNoteRepositoryTest {
    private lateinit var db: PkcoDatabase
    private lateinit var repository: RoomNoteRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PkcoDatabase::class.java,
        ).build()
        repository = RoomNoteRepository(db) { now }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createNote_withCategoryAndTwoTags_readsBackAllFields() = runBlocking {
        val categoryId = repository.createCategory("Work")
        val id = repository.createNote("Title", "Body https://example.com", categoryId, listOf("kotlin", "room"))

        val details = repository.observeNote(id).first()
        assertNotNull(details)
        details!!
        assertEquals(id, details.note.id)
        assertEquals("Title", details.note.title)
        assertEquals("Body https://example.com", details.note.body)
        assertEquals(categoryId, details.note.categoryId)
        assertEquals("Work", details.category?.name)
        assertEquals(setOf("kotlin", "room"), details.tags.map { it.name }.toSet())
        assertEquals(1_000L, details.note.createdAt)
        assertEquals(1_000L, details.note.updatedAt)
        assertNull(details.note.deletedAt)
    }

    @Test
    fun tagNames_reuseExistingTagIgnoringCase_andSkipBlanks() = runBlocking {
        val first = repository.createNote("A", "", null, listOf("Kotlin", "  ", ""))
        val second = repository.createNote("B", "", null, listOf("kotlin", " KOTLIN "))

        val tags = repository.observeTags().first()
        assertEquals(1, tags.size)
        assertEquals("Kotlin", tags.single().name)
        assertEquals(listOf(tags.single()), repository.observeNote(first).first()!!.tags)
        assertEquals(listOf(tags.single()), repository.observeNote(second).first()!!.tags)
    }

    @Test
    fun updateNote_changesFieldsAndTags_keepsCreatedAt_advancesUpdatedAt() = runBlocking {
        val work = repository.createCategory("Work")
        val home = repository.createCategory("Home")
        val id = repository.createNote("Old", "old body", work, listOf("a", "b"))

        now = 2_000L
        repository.updateNote(id, "New", "new body", home, listOf("b", "c"))

        val note = repository.observeNote(id).first()!!
        assertEquals("New", note.note.title)
        assertEquals("new body", note.note.body)
        assertEquals(home, note.note.categoryId)
        assertEquals(setOf("b", "c"), note.tags.map { it.name }.toSet())
        assertEquals(1_000L, note.note.createdAt)
        assertEquals(2_000L, note.note.updatedAt)
    }

    @Test
    fun updateNote_withSameCategoryAndTags_keepsThem() = runBlocking {
        val work = repository.createCategory("Work")
        val id = repository.createNote("Old", "body", work, listOf("a", "b"))
        val before = repository.observeNote(id).first()!!

        now = 2_000L
        repository.updateNote(id, "New", "body", before.note.categoryId, before.tags.map { it.name })

        val after = repository.observeNote(id).first()!!
        assertEquals("New", after.note.title)
        assertEquals(work, after.note.categoryId)
        assertEquals(before.tags.toSet(), after.tags.toSet())
        assertEquals(2, repository.observeTags().first().size)
    }

    @Test
    fun updateNote_withNoTagsAndNoCategory_unlinksThem_keepsTagRows() = runBlocking {
        val work = repository.createCategory("Work")
        val id = repository.createNote("Note", "", work, listOf("a", "b"))

        repository.updateNote(id, "Note", "", null, emptyList())

        val note = repository.observeNote(id).first()!!
        assertNull(note.note.categoryId)
        assertNull(note.category)
        assertTrue(note.tags.isEmpty())
        // Unused tags and the category stay in the database.
        assertEquals(setOf("a", "b"), repository.observeTags().first().map { it.name }.toSet())
        assertEquals(listOf(work), repository.observeCategories().first().map { it.id })
    }

    @Test
    fun renameCategory_toFreeName_noteKeepsPointingToIt() = runBlocking {
        val work = repository.createCategory("Work")
        val id = repository.createNote("Note", "", work, emptyList())

        repository.renameCategory(work, " Job ")

        val note = repository.observeNote(id).first()!!
        assertEquals(work, note.note.categoryId)
        assertEquals("Job", note.category?.name)
    }

    @Test
    fun observeNotes_emitsAgain_whenCategoryIsRenamedOrDeleted() = runBlocking {
        val work = repository.createCategory("Work")
        repository.createNote("Note", "", work, listOf("a"))
        val emissions = Channel<NoteWithDetails>(Channel.UNLIMITED)
        val collector = launch { repository.observeNotes().collect { emissions.send(it.single()) } }

        // Room may emit the same list more than once, so skip emissions until one matches.
        suspend fun awaitNote(matches: (NoteWithDetails) -> Boolean) = withTimeout(5_000) {
            do {
                val note = emissions.receive()
            } while (!matches(note))
        }

        awaitNote { it.category?.name == "Work" }
        repository.renameCategory(work, "Job")
        awaitNote { it.category?.name == "Job" }
        repository.deleteCategory(work)
        awaitNote { it.note.categoryId == null && it.category == null }
        collector.cancel()
    }

    @Test
    fun deleteNote_isSoft_hidesNoteButKeepsRow() = runBlocking {
        val id = repository.createNote("Gone", "", null, listOf("x"))

        now = 3_000L
        repository.deleteNote(id)

        assertTrue(repository.observeNotes().first().isEmpty())
        assertNull(repository.observeNote(id).first())
        val row = db.noteDao().getById(id)
        assertNotNull(row)
        assertEquals(3_000L, row!!.deletedAt)
    }

    @Test
    fun deleteCategory_leavesNotesUncategorized() = runBlocking {
        val categoryId = repository.createCategory("Temp")
        val id = repository.createNote("Note", "", categoryId, emptyList())

        repository.deleteCategory(categoryId)

        val note = repository.observeNote(id).first()!!
        assertNull(note.note.categoryId)
        assertNull(note.category)
        assertTrue(repository.observeCategories().first().isEmpty())
    }

    @Test
    fun categoryNames_duplicateIgnoringCaseAndBlank_areRejected() = runBlocking {
        val work = repository.createCategory("Work")
        val home = repository.createCategory("Home")

        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.createCategory(" work ") } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.createCategory("   ") } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.renameCategory(home, "WORK") } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.renameCategory(home, "") } }

        // Renaming a category to a different case of its own name is allowed.
        repository.renameCategory(work, " WORK ")
        assertEquals(listOf("Home", "WORK"), repository.observeCategories().first().map { it.name })
    }

    @Test
    fun observeNotes_returnsMostRecentlyEditedFirst() = runBlocking {
        val older = repository.createNote("Older", "", null, emptyList())
        now = 2_000L
        val newer = repository.createNote("Newer", "", null, emptyList())
        assertEquals(listOf(newer, older), repository.observeNotes().first().map { it.note.id })

        now = 3_000L
        repository.updateNote(older, "Older, edited", "", null, emptyList())
        assertEquals(listOf(older, newer), repository.observeNotes().first().map { it.note.id })
    }

    @Test
    fun reservedColumns_areNullAfterCreation() = runBlocking {
        val id = repository.createNote("Title", "Body", null, emptyList())

        val note = db.noteDao().getById(id)!!
        assertNull(note.ownerId)
        assertNull(note.sourceImageUri)
        assertNull(note.extractedText)
        assertNull(note.latitude)
        assertNull(note.longitude)
        assertNull(note.deletedAt)
    }
}
