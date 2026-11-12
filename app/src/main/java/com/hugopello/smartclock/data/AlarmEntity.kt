package com.hugopello.smartclock.data

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
)

fun AlarmSettings.toEntity() = AlarmEntity(
    arrivalMinuteOfDay = arrival.toSecondOfDay() / 60,
    preparationMinutes = preparation.toMinutes().toInt(),
    usualTravelMinutes = usualTravel.toMinutes().toInt(),
    destination = destination,
)
