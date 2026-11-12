package com.hugopello.smartclock.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * exportSchema = true: Room writes each version's schema as JSON to `app/schemas/` (committed).
 * Those files are the history needed to write and test future migrations.
 * Version 1 was never exported, so 1 → 2 is a hand-written migration.
 * 2 → 3 (new `origin` column with a default + new table) is simple enough for an [AutoMigration]:
 * Room compares 2.json with 3.json and generates the SQL at compile time.
 */
@Database(
    entities = [AlarmEntity::class, TrafficAdjustmentEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 2, to = 3)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "smart-clock.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/** v2 adds the on/off switch. Existing alarms start switched off, so nothing rings unexpectedly. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarm ADD COLUMN enabled INTEGER NOT NULL DEFAULT 0")
    }
}
