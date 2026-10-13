package com.example.pkco.data.repository

import androidx.room.withTransaction
import com.example.pkco.data.PkcoDatabase
import com.example.pkco.data.entity.Category
import com.example.pkco.data.entity.Note
import com.example.pkco.data.entity.NoteTag
import com.example.pkco.data.entity.NoteWithDetails
import com.example.pkco.data.entity.Tag
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Room implementation of [NoteRepository]. [clock] returns the current UTC time in epoch milliseconds. */
class RoomNoteRepository(
    private val db: PkcoDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : NoteRepository {
    private val noteDao = db.noteDao()
    private val categoryDao = db.categoryDao()
    private val tagDao = db.tagDao()

    override fun observeNotes(): Flow<List<NoteWithDetails>> = noteDao.observeActive()

    override fun observeNote(id: String): Flow<NoteWithDetails?> = noteDao.observeActive(id)

    override suspend fun createNote(
        title: String,
        body: String,
        categoryId: String?,
        tagNames: List<String>,
    ): String {
        val id = newId()
        val now = clock()
        db.withTransaction {
            noteDao.insert(
                Note(id = id, title = title, body = body, categoryId = categoryId, createdAt = now, updatedAt = now)
            )
            linkTags(id, tagNames)
        }
        return id
    }

    override suspend fun updateNote(
        id: String,
        title: String,
        body: String,
        categoryId: String?,
        tagNames: List<String>,
    ) {
        db.withTransaction {
            val existing = noteDao.getById(id)
            require(existing != null && existing.deletedAt == null) { "Note $id does not exist or is deleted." }
            noteDao.update(existing.copy(title = title, body = body, categoryId = categoryId, updatedAt = clock()))
            noteDao.deleteNoteTags(id)
            linkTags(id, tagNames)
        }
    }

    override suspend fun deleteNote(id: String) = noteDao.softDelete(id, clock())

    override fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    override suspend fun createCategory(name: String): String {
        val trimmed = validCategoryName(name)
        val id = newId()
        db.withTransaction {
            require(categoryDao.findByName(trimmed) == null) { "A category named \"$trimmed\" already exists." }
            categoryDao.insert(Category(id = id, name = trimmed))
        }
        return id
    }

    override suspend fun renameCategory(id: String, name: String) {
        val trimmed = validCategoryName(name)
        db.withTransaction {
            val other = categoryDao.findByName(trimmed)
            require(other == null || other.id == id) { "A category named \"$trimmed\" already exists." }
            categoryDao.rename(id, trimmed)
        }
    }

    override suspend fun deleteCategory(id: String) = categoryDao.delete(id)

    override fun observeTags(): Flow<List<Tag>> = tagDao.observeAll()

    /** Links the note to the named tags, creating missing ones. Call inside a transaction. */
    private suspend fun linkTags(noteId: String, tagNames: List<String>) {
        val names = tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        val links = names.map { name ->
            val tag = tagDao.findByName(name) ?: Tag(id = newId(), name = name).also { tagDao.insert(it) }
            NoteTag(noteId = noteId, tagId = tag.id)
        }
        noteDao.insertNoteTags(links)
    }

    private fun validCategoryName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Category name must not be blank." } }

    private fun newId(): String = UUID.randomUUID().toString()
}
