package com.example.pkco.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A free tag. Names are unique, ignoring case. */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class Tag(
    @PrimaryKey val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)
