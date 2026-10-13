package com.example.pkco.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A category; each note has at most one. Names are unique, ignoring case. */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class Category(
    @PrimaryKey val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)
