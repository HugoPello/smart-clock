package com.hugopello.smartclock.domain

import java.time.Duration
import java.time.LocalTime

/**
 * Everything the calculator needs. No Android types on purpose: this runs in plain JVM unit tests.
 *
 * @property arrival when the user must arrive at the destination.
 * @property preparation time the user needs between waking up and leaving.
 * @property usualTravel normal travel time (the one the original alarm is based on).
 * @property todayTravel travel time reported by the traffic API today; `null` if the query failed.
 * @property currentAlarm alarm already scheduled (after a previous query); `null` on the first query.
 */
data class AlarmInput(
    val arrival: LocalTime,
    val preparation: Duration,
    val usualTravel: Duration,
    val todayTravel: Duration?,
    val currentAlarm: LocalTime? = null,
)

/**
 * Safety rules. The user can tighten them (the "user window").
 *
 * @property maxEarlier the alarm never moves more than this before the original time.
 * @property maxLater the alarm never moves more than this after the original time.
 * @property threshold changes smaller than this (vs the current alarm) are ignored to avoid oscillation.
 */
data class AlarmLimits(
    val maxEarlier: Duration = Duration.ofMinutes(30),
    val maxLater: Duration = Duration.ofMinutes(15),
    val threshold: Duration = Duration.ofMinutes(3),
)

/** Why the alarm ended up at [AlarmDecision.alarm]. Useful for the UI and for tests. */
enum class DecisionReason {
    /** No traffic data: keep the original time (or the current alarm if there is one). */
    NO_TRAFFIC_DATA,

    /** Moved to exactly match today's traffic. */
    ADJUSTED,

    /** Wanted to move more than [AlarmLimits.maxEarlier]; capped. */
    CAPPED_EARLIER,

    /** Wanted to move more than [AlarmLimits.maxLater]; capped. */
    CAPPED_LATER,

    /** Change smaller than [AlarmLimits.threshold]: the current alarm stays. */
    BELOW_THRESHOLD,
}

data class AlarmDecision(
    val alarm: LocalTime,
    val reason: DecisionReason,
)

/**
 * Pure function: same input → same output, no I/O, no clock, no Android.
 *
 * Steps:
 * 1. original = arrival − preparation − usualTravel
 * 2. No todayTravel → keep currentAlarm ?: original  (NO_TRAFFIC_DATA)
 * 3. target   = arrival − preparation − todayTravel
 * 4. Cap the shift (target − original) to [−maxEarlier, +maxLater]   (CAPPED_* if it was cut)
 * 5. If |candidate − (currentAlarm ?: original)| < threshold → keep the current one (BELOW_THRESHOLD)
 * 6. Otherwise return the candidate (ADJUSTED or CAPPED_*)
 */
fun calculateAlarm(
    input: AlarmInput,
    limits: AlarmLimits = AlarmLimits(),
): AlarmDecision {
    val original = input.arrival - input.preparation - input.usualTravel
    val current = input.currentAlarm ?: original

    val todayTravel = input.todayTravel
        ?: return AlarmDecision(current, DecisionReason.NO_TRAFFIC_DATA)

    // Negative shift = alarm earlier. Computed from durations (not LocalTimes) so midnight can't break it.
    val wantedShift = input.usualTravel - todayTravel
    val minShift = limits.maxEarlier.negated()
    val maxShift = limits.maxLater
    val (shift, reason) = when {
        wantedShift < minShift -> minShift to DecisionReason.CAPPED_EARLIER
        wantedShift > maxShift -> maxShift to DecisionReason.CAPPED_LATER
        else -> wantedShift to DecisionReason.ADJUSTED
    }
    val candidate = original + shift

    return if (circularDistance(candidate, current) < limits.threshold) {
        AlarmDecision(current, DecisionReason.BELOW_THRESHOLD)
    } else {
        AlarmDecision(candidate, reason)
    }
}

/** Distance between two times on a 24h clock: 23:58 and 00:01 are 3 min apart, not 23h57. */
private fun circularDistance(a: LocalTime, b: LocalTime): Duration {
    val diff = Duration.between(a, b).abs()
    val day = Duration.ofDays(1)
    return if (diff > day.dividedBy(2)) day - diff else diff
}
