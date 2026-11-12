package com.hugopello.smartclock.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarm WHERE id = ${AlarmEntity.SINGLE_ID}")
    suspend fun get(): AlarmEntity?

    /** Insert or replace: there is only ever one row. */
    @Upsert
    suspend fun upsert(alarm: AlarmEntity)

    @Query("SELECT * FROM traffic_adjustment WHERE id = ${AlarmEntity.SINGLE_ID}")
    suspend fun getAdjustment(): TrafficAdjustmentEntity?

    @Upsert
    suspend fun upsertAdjustment(adjustment: TrafficAdjustmentEntity)

    @Query("DELETE FROM traffic_adjustment")
    suspend fun clearAdjustment()

    /**
     * New settings make the stored traffic result meaningless (other route, other time).
     * Both writes in one transaction: either both happen or neither does.
     */
    @Transaction
    suspend fun saveSettings(alarm: AlarmEntity) {
        upsert(alarm)
        clearAdjustment()
    }
}
