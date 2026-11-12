package com.hugopello.smartclock.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Result of the traffic check for one specific day's alarm.
 *
 * @property occurrenceDate date of the *original* alarm this applies to; any other day ignores it.
 * @property shift how far the ring moved from the original time (negative = earlier).
 * @property todayTravel travel time reported by the API; `null` if the check failed.
 */
data class TrafficAdjustment(
    val occurrenceDate: LocalDate,
    val shift: Duration,
    val todayTravel: Duration?,
    val reason: DecisionReason,
    val checkedAt: Instant,
)

/**
 * @property occurrence when the alarm would ring with no traffic data (the "original" one).
 * @property ring when it will actually ring (occurrence + traffic shift, if already checked).
 * @property trafficCheck when to query traffic; `null` once this occurrence has been checked.
 */
data class AlarmPlan(
    val occurrence: ZonedDateTime,
    val ring: ZonedDateTime,
    val trafficCheck: ZonedDateTime?,
)

/**
 * Query traffic this long before the *earliest* possible ring (original − maxEarlier),
 * so even a −30 min adjustment is known before it's needed.
 */
val TRAFFIC_CHECK_LEAD: Duration = Duration.ofMinutes(15)

/**
 * Pure: what should be registered with the system, seen from [from].
 *
 * The search starts [AlarmLimits.maxLater] earlier than [from]: if the original time (07:00) has just
 * passed but traffic moved today's ring to 07:15, today's alarm is still pending and must be kept.
 * Whatever is found must ring strictly after [from]; otherwise that day is done (it already rang,
 * possibly early because of traffic) and the next day is planned.
 */
fun planAlarm(
    settings: AlarmSettings,
    adjustment: TrafficAdjustment?,
    from: ZonedDateTime,
    limits: AlarmLimits = AlarmLimits(),
): AlarmPlan {
    val first = planFor(nextOccurrence(settings.originalAlarm, from - limits.maxLater), adjustment, limits)
    if (first.ring.isAfter(from)) return first
    return planFor(nextOccurrence(settings.originalAlarm, first.occurrence), adjustment, limits)
}

private fun planFor(occurrence: ZonedDateTime, adjustment: TrafficAdjustment?, limits: AlarmLimits): AlarmPlan {
    val applied = adjustment?.takeIf { it.occurrenceDate == occurrence.toLocalDate() }
    return AlarmPlan(
        occurrence = occurrence,
        ring = occurrence + (applied?.shift ?: Duration.ZERO),
        trafficCheck = if (applied == null) occurrence - limits.maxEarlier - TRAFFIC_CHECK_LEAD else null,
    )
}
