package com.example.pkco.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** A note together with its category (if any) and its tags. */
data class NoteWithDetails(
    @Embedded val note: Note,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: Category?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(NoteTag::class, parentColumn = "noteId", entityColumn = "tagId"),
    )
    val tags: List<Tag>,
)
