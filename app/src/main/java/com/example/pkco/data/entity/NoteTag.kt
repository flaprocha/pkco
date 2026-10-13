package com.example.pkco.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Many-to-many link between notes and tags. */
@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("noteId"), Index("tagId")],
)
data class NoteTag(
    val noteId: String,
    val tagId: String,
)
