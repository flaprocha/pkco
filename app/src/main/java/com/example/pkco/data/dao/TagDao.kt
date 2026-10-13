package com.example.pkco.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pkco.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name")
    fun observeAll(): Flow<List<Tag>>

    /** Case-insensitive through the column's NOCASE collation. */
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Tag?

    @Insert
    suspend fun insert(tag: Tag)
}
