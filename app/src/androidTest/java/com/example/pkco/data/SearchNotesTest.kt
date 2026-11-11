package com.example.pkco.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pkco.data.repository.RoomNoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchNotesTest {
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

    /** Creates a note at time [at]. */
    private suspend fun note(
        at: Long,
        title: String = "",
        body: String = "",
        categoryId: String? = null,
        tags: List<String> = emptyList(),
    ): String {
        now = at
        return repository.createNote(title, body, categoryId, tags)
    }

    private suspend fun search(query: String, categoryId: String? = null, tagId: String? = null): List<String> =
        repository.searchNotes(query, categoryId, tagId).first().map { it.note.id }

    private suspend fun tagId(name: String): String = repository.observeTags().first().single { it.name == name }.id

    @Test
    fun titleMatch_outranksTagMatch_whichOutranksBodyMatch() = runBlocking {
        // The older notes score higher, so recency cannot explain the order.
        val title = note(1_000, title = "Kotlin basics")
        val tag = note(2_000, title = "Other", tags = listOf("kotlin"))
        val body = note(3_000, title = "Third", body = "about kotlin")
        note(4_000, title = "Unrelated", body = "nothing here")

        assertEquals(listOf(title, tag, body), search("kotlin"))
    }

    @Test
    fun categoryNameMatch_scoresTwo_andTagPlusCategoryCountsOnce() = runBlocking {
        val kotlin = repository.createCategory("Kotlin")
        val title = note(1_000, title = "kotlin title")
        val tagAndCategory = note(2_000, title = "A", categoryId = kotlin, tags = listOf("kotlin"))
        val category = note(3_000, title = "B", categoryId = kotlin)
        val body = note(4_000, title = "C", body = "kotlin")

        // Title 3 > category 2 (and tag + category is still 2; the newer one wins the tie) > body 1.
        assertEquals(listOf(title, category, tagAndCategory, body), search("kotlin"))
    }

    @Test
    fun pointsAddUpAcrossWords() = runBlocking {
        val both = note(1_000, title = "Kotlin", body = "for android")      // 3 + 1 = 4
        val titleOnly = note(2_000, title = "Android")                      // 3
        val bodyBoth = note(3_000, title = "x", body = "kotlin and android") // 1 + 1 = 2

        assertEquals(listOf(both, titleOnly, bodyBoth), search("kotlin android"))
    }

    @Test
    fun partialMatch_ignoringCase() = runBlocking {
        val id = note(1_000, title = "Kotlin")
        note(2_000, title = "Java")

        assertEquals(listOf(id), search("kot"))
        assertEquals(listOf(id), search("KOT"))
        assertEquals(listOf(id), search("tLi"))
    }

    @Test
    fun equalScores_mostRecentlyEditedFirst_thenMostRecentlyCreated() = runBlocking {
        val first = note(1_000, title = "kotlin one")
        val second = note(2_000, title = "kotlin two")
        val third = note(2_500, title = "kotlin three")
        assertEquals(listOf(third, second, first), search("kotlin"))

        now = 3_000
        repository.updateNote(first, "kotlin one", "edited", null, emptyList())
        repository.updateNote(second, "kotlin two", "edited", null, emptyList())
        // first and second now share updatedAt 3000; second was created later.
        assertEquals(listOf(second, first, third), search("kotlin"))
    }

    @Test
    fun zeroScoreAndDeletedNotes_areHidden() = runBlocking {
        val kept = note(1_000, title = "kotlin kept")
        val deleted = note(2_000, title = "kotlin deleted")
        note(3_000, title = "java", body = "nothing")
        repository.deleteNote(deleted)

        assertEquals(listOf(kept), search("kotlin"))
        assertEquals(emptyList<String>(), search("python"))
        assertEquals(2, search("").size)
    }

    @Test
    fun blankQuery_returnsAllNotes_inObserveNotesOrder() = runBlocking {
        note(1_000, title = "a")
        note(2_000, title = "b")
        val deleted = note(3_000, title = "c")
        note(4_000, title = "d")
        repository.deleteNote(deleted)

        val expected = repository.observeNotes().first().map { it.note.id }
        assertEquals(3, expected.size)
        assertEquals(expected, search(""))
        assertEquals(expected, search("   "))
    }

    @Test
    fun filterByCategory() = runBlocking {
        val work = repository.createCategory("Work")
        val home = repository.createCategory("Home")
        val inWork = note(1_000, title = "w", categoryId = work)
        note(2_000, title = "h", categoryId = home)
        note(3_000, title = "none")

        assertEquals(listOf(inWork), search("", categoryId = work))
    }

    @Test
    fun filterByTag() = runBlocking {
        val tagged = note(1_000, title = "a", tags = listOf("study", "kotlin"))
        note(2_000, title = "b", tags = listOf("kotlin"))
        val alsoTagged = note(3_000, title = "c", tags = listOf("Study"))

        assertEquals(listOf(alsoTagged, tagged), search("", tagId = tagId("study")))
    }

    @Test
    fun filtersAndSearch_combineWithAnd() = runBlocking {
        val work = repository.createCategory("Work")
        val match = note(1_000, title = "kotlin", categoryId = work, tags = listOf("study"))
        note(2_000, title = "kotlin", categoryId = work)                  // no tag
        note(3_000, title = "kotlin", tags = listOf("study"))             // no category
        note(4_000, title = "java", categoryId = work, tags = listOf("study")) // no word match
        val study = tagId("study")

        assertEquals(listOf(match), search("kotlin", categoryId = work, tagId = study))
        assertEquals(2, search("kotlin", categoryId = work).size)
        assertEquals(2, search("kotlin", tagId = study).size)
    }

    @Test
    fun percentAndUnderscore_matchLiterally() = runBlocking {
        val percent = note(1_000, title = "50% off")
        val underscore = note(2_000, title = "snake_case")
        val backslash = note(3_000, title = "c:\\temp")
        note(4_000, title = "plain words")

        assertEquals(listOf(percent), search("%"))
        assertEquals(listOf(underscore), search("_"))
        assertEquals(listOf(backslash), search("\\"))
    }
}
