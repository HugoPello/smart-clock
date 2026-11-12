package com.hugopello.smartclock.domain

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * The next moment, strictly after [now], when the wall clock shows [time].
 *
 * Pure (no `now()` inside): tests pass any instant and time zone.
 * Daylight saving is handled by [ZonedDateTime.of]: a time inside the spring-forward gap
 * (e.g. 02:30 when clocks jump 02:00 → 03:00) is moved forward by the gap length (→ 03:30).
 */
fun nextOccurrence(time: LocalTime, now: ZonedDateTime): ZonedDateTime {
    val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
    return if (today.isAfter(now)) today else ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
}
