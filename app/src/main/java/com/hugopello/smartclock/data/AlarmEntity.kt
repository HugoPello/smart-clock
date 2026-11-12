package com.hugopello.smartclock.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hugopello.smartclock.domain.AlarmSettings
import com.hugopello.smartclock.domain.DecisionReason
import com.hugopello.smartclock.domain.TrafficAdjustment
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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
    // Added in v3 by an AutoMigration: the default value is what existing rows get.
    @ColumnInfo(defaultValue = "") val origin: String,
) {
    companion object {
        const val SINGLE_ID = 1
    }
}

/**
 * Result of the last traffic check (single row, like [AlarmEntity]).
 * Kept in the database, not in memory: the check runs in the background, maybe in a process that
 * dies right after, and any later sync (app opened, reboot) must still see it.
 */
@Entity(tableName = "traffic_adjustment")
data class TrafficAdjustmentEntity(
    @PrimaryKey val id: Int = AlarmEntity.SINGLE_ID,
    val occurrenceEpochDay: Long,
    /** Seconds, not minutes: a late check can move the ring to "now + 1 min", not a whole minute. */
    val shiftSeconds: Long,
    val todayTravelSeconds: Long?,
    /** Enum name. Renaming a [DecisionReason] value would need a migration. */
    val reason: String,
    val checkedAtEpochSecond: Long,
)

fun AlarmEntity.toDomain() = AlarmSettings(
    arrival = LocalTime.ofSecondOfDay(arrivalMinuteOfDay * 60L),
    preparation = Duration.ofMinutes(preparationMinutes.toLong()),
    usualTravel = Duration.ofMinutes(usualTravelMinutes.toLong()),
    destination = destination,
    enabled = enabled,
    origin = origin,
)

fun AlarmSettings.toEntity() = AlarmEntity(
    arrivalMinuteOfDay = arrival.toSecondOfDay() / 60,
    preparationMinutes = preparation.toMinutes().toInt(),
    usualTravelMinutes = usualTravel.toMinutes().toInt(),
    destination = destination,
    enabled = enabled,
    origin = origin,
)

fun TrafficAdjustmentEntity.toDomain() = TrafficAdjustment(
    occurrenceDate = LocalDate.ofEpochDay(occurrenceEpochDay),
    shift = Duration.ofSeconds(shiftSeconds),
    todayTravel = todayTravelSeconds?.let(Duration::ofSeconds),
    reason = DecisionReason.valueOf(reason),
    checkedAt = Instant.ofEpochSecond(checkedAtEpochSecond),
)

fun TrafficAdjustment.toEntity() = TrafficAdjustmentEntity(
    occurrenceEpochDay = occurrenceDate.toEpochDay(),
    shiftSeconds = shift.seconds,
    todayTravelSeconds = todayTravel?.seconds,
    reason = reason.name,
    checkedAtEpochSecond = checkedAt.epochSecond,
)
