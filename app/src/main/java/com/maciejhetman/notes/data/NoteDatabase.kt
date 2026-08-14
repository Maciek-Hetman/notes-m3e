package com.maciejhetman.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, Folder::class], version = 3, exportSchema = true)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var Instance: NoteDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folders` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`parentFolderId` INTEGER, " +
                            "`name` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`parentFolderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_parentFolderId` ON `folders` (`parentFolderId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`folderId` INTEGER, " +
                            "`title` TEXT NOT NULL, " +
                            "`content` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "`modifiedAt` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("INSERT INTO `notes_new` (`id`, `title`, `content`, `createdAt`, `modifiedAt`) SELECT `id`, `title`, `content`, `createdAt`, `modifiedAt` FROM `notes`")
                db.execSQL("DROP TABLE `notes`")
                db.execSQL("ALTER TABLE `notes_new` RENAME TO `notes`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_folderId` ON `notes` (`folderId`)")
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return Instance ?: synchronized(this) {
                // The next version bump must ship a real Migration; the v1→v2 change
                // shipped without one, so that history is unrecoverable retroactively.
                Room.databaseBuilder(context, NoteDatabase::class.java, "note_database")
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
