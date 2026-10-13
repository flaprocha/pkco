package com.example.pkco.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pkco.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<Category>>

    /** Case-insensitive through the column's NOCASE collation. */
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Category?

    @Insert
    suspend fun insert(category: Category)

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)
}
