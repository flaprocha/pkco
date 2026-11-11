package com.example.pkco.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.pkco.data.entity.Category
import com.example.pkco.data.entity.Note
import com.example.pkco.data.entity.NoteTag
import com.example.pkco.data.entity.NoteWithDetails
import com.example.pkco.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY updatedAt DESC, createdAt DESC")
    fun observeActive(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL")
    fun observeActive(id: String): Flow<NoteWithDetails?>

    /** Runs a query built by `SearchQueryBuilder`; it emits again when any of the four tables changes. */
    @Transaction
    @RawQuery(observedEntities = [Note::class, Category::class, Tag::class, NoteTag::class])
    fun search(query: SupportSQLiteQuery): Flow<List<NoteWithDetails>>

    /** Returns the row even when its reserved deletedAt column is set. */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: String): Note?

    @Insert
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    /** Removes the note row. Call after [deleteNoteTags], inside a transaction. */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTags(links: List<NoteTag>)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteNoteTags(noteId: String)

    @Query(PURGE_NOTE_TAGS_SQL)
    suspend fun deleteSoftDeletedNoteTags()

    @Query(PURGE_NOTES_SQL)
    suspend fun deleteSoftDeletedNotes()

    /** Erases notes soft-deleted by versions before Sprint 9, their note_tags rows first. */
    @Transaction
    suspend fun purgeSoftDeleted() {
        deleteSoftDeletedNoteTags()
        deleteSoftDeletedNotes()
    }

    companion object {
        /** Shared with `PkcoDatabase`'s open callback, which runs the same purge on the raw database. */
        const val PURGE_NOTE_TAGS_SQL =
            "DELETE FROM note_tags WHERE noteId IN (SELECT id FROM notes WHERE deletedAt IS NOT NULL)"
        const val PURGE_NOTES_SQL = "DELETE FROM notes WHERE deletedAt IS NOT NULL"
    }
}
