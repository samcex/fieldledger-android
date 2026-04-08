package com.indie.shiftledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [JobEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class JobDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao

    companion object {
        fun build(context: Context): JobDatabase {
            return Room.databaseBuilder(
                context,
                JobDatabase::class.java,
                "field-ledger.db",
            ).fallbackToDestructiveMigration().build()
        }
    }
}
