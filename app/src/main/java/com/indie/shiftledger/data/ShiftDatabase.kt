package com.indie.shiftledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [JobEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class JobDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao

    companion object {
        private val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE jobs ADD COLUMN paymentDueEpochDay INTEGER")
                db.execSQL("ALTER TABLE jobs ADD COLUMN reminderEpochDay INTEGER")
                db.execSQL("ALTER TABLE jobs ADD COLUMN reminderNote TEXT NOT NULL DEFAULT ''")
            }
        }

        fun build(context: Context): JobDatabase {
            return Room.databaseBuilder(
                context,
                JobDatabase::class.java,
                "field-ledger.db",
            ).addMigrations(Migration2To3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
