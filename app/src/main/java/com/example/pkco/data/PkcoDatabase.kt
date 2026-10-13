package com.example.pkco.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pkco.data.dao.CategoryDao
import com.example.pkco.data.dao.NoteDao
import com.example.pkco.data.dao.TagDao
import com.example.pkco.data.entity.Category
import com.example.pkco.data.entity.Note
import com.example.pkco.data.entity.NoteTag
import com.example.pkco.data.entity.Tag

/** PKCO's Room database. Schema changes need a version bump and a migration; no destructive fallback. */
@Database(
    entities = [Note::class, Category::class, Tag::class, NoteTag::class],
    version = 1,
    exportSchema = true,
)
abstract class PkcoDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao

    companion object {
        private const val NAME = "pkco.db"

        @Volatile
        private var instance: PkcoDatabase? = null

        fun getInstance(context: Context): PkcoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PkcoDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }
    }
}
