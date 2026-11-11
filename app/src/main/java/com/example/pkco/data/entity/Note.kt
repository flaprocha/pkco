package com.example.pkco.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A text note. Timestamps are UTC epoch milliseconds. [deletedAt] is reserved and unused: deleting a note removes
 * the row, and rows soft-deleted by versions before Sprint 9 are purged when the database opens.
 * [ownerId], [sourceImageUri], [extractedText], [latitude] and [longitude] are reserved for
 * Phase 2 and stay null in Phase 1.
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class Note(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val categoryId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val ownerId: String? = null,
    val sourceImageUri: String? = null,
    val extractedText: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
