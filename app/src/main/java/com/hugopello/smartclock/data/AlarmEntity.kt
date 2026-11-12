package com.hugopello.smartclock.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hugopello.smartclock.domain.AlarmSettings
import java.time.Duration
import java.time.LocalTime

/**
 * Database row. MVP has a single alarm, so the id is always [SINGLE_ID].
 * Times are stored as plain minutes: simple, sortable, no TypeConverters needed.
 */
@Entity(tableName = "alarm")
data class AlarmEntity(
    @PrimaryKey val id: Int = SINGLE_ID,
    val arrivalMinuteOfDay: Int,
    val preparationMinutes: Int,
    val usualTravelMinutes: Int,
    val destination: String,
    // Must match MIGRATION_1_2 exactly: Room checks the real table against this class on open.
    @ColumnInfo(defaultValue = "0") val enabled: Boolean,
) {
    companion object {
        const val SINGLE_ID = 1
    }
}

fun AlarmEntity.toDomain() = AlarmSettings(
    arrival = LocalTime.ofSecondOfDay(arrivalMinuteOfDay * 60L),
    preparation = Duration.ofMinutes(preparationMinutes.toLong()),
    usualTravel = Duration.ofMinutes(usualTravelMinutes.toLong()),
    destination = destination,
    enabled = enabled,
)

fun AlarmSettings.toEntity() = AlarmEntity(
    arrivalMinuteOfDay = arrival.toSecondOfDay() / 60,
    preparationMinutes = preparation.toMinutes().toInt(),
    usualTravelMinutes = usualTravel.toMinutes().toInt(),
    destination = destination,
    enabled = enabled,
)
