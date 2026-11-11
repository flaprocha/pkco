package com.example.pkco.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
                ).addCallback(PurgeSoftDeletedOnOpen).build().also { instance = it }
            }

        /**
         * Erases notes soft-deleted by versions before Sprint 9 (deletedAt set) and their note_tags rows, in one
         * transaction, each time the database opens. It runs [NoteDao.purgeSoftDeleted]'s SQL on the raw database,
         * because calling a DAO from inside onOpen would reopen the database recursively.
         */
        internal object PurgeSoftDeletedOnOpen : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.beginTransaction()
                try {
                    db.execSQL(NoteDao.PURGE_NOTE_TAGS_SQL)
                    db.execSQL(NoteDao.PURGE_NOTES_SQL)
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }
}
