package com.hugopello.smartclock.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarm WHERE id = ${AlarmEntity.SINGLE_ID}")
    suspend fun get(): AlarmEntity?

    /** Insert or replace: there is only ever one row. */
    @Upsert
    suspend fun upsert(alarm: AlarmEntity)
}
