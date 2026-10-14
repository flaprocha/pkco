package com.example.pkco.data.repository

import com.example.pkco.data.entity.Category
import com.example.pkco.data.entity.NoteWithDetails
import com.example.pkco.data.entity.Tag
import kotlinx.coroutines.flow.Flow

/** The only data entry point for the UI. Reads are Flows; writes are suspend functions. */
interface NoteRepository {
    /** Notes that are not deleted, most recently edited first. */
    fun observeNotes(): Flow<List<NoteWithDetails>>

    /**
     * Notes that are not deleted and match [query] and the filters. Each word of [query] (split on whitespace,
     * at most 8) scores, as a partial match ignoring case: title 3, any tag or the category name 2, body 1.
     * Notes scoring 0 are left out; the rest come by score, then most recently edited. A blank [query] returns
     * every note in the [observeNotes] order. [categoryId] and [tagId], when not null, keep only notes with that
     * category or that tag.
     */
    fun searchNotes(query: String, categoryId: String?, tagId: String?): Flow<List<NoteWithDetails>>

    /** One note that is not deleted, or null. */
    fun observeNote(id: String): Flow<NoteWithDetails?>

    /** Creates a note and returns its id. Blank tag names are ignored; existing tags are reused, ignoring case. */
    suspend fun createNote(title: String, body: String, categoryId: String?, tagNames: List<String>): String

    /** Updates a note and replaces its tag set. Throws [IllegalArgumentException] if the note is missing or deleted. */
    suspend fun updateNote(id: String, title: String, body: String, categoryId: String?, tagNames: List<String>)

    /** Soft delete: sets deletedAt; the row stays in the database. */
    suspend fun deleteNote(id: String)

    fun observeCategories(): Flow<List<Category>>

    /** Returns the new id. Throws [IllegalArgumentException] if the name is blank or already used, ignoring case. */
    suspend fun createCategory(name: String): String

    /** Throws [IllegalArgumentException] if the name is blank or used by another category, ignoring case. */
    suspend fun renameCategory(id: String, name: String)

    /** Deletes the category; its notes become uncategorized. */
    suspend fun deleteCategory(id: String)

    fun observeTags(): Flow<List<Tag>>
}
