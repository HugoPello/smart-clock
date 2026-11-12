package com.hugopello.smartclock.traffic

import com.hugopello.smartclock.alarm.AlarmSync
import com.hugopello.smartclock.data.AlarmRepository
import com.hugopello.smartclock.domain.AlarmInput
import com.hugopello.smartclock.domain.AlarmLimits
import com.hugopello.smartclock.domain.TrafficAdjustment
import com.hugopello.smartclock.domain.calculateAlarm
import com.hugopello.smartclock.domain.planAlarm
import java.time.Duration
import java.time.ZonedDateTime

enum class CheckOutcome {
    /** Adjustment stored and alarm rescheduled (even if traffic was unavailable: then the shift is 0). */
    DONE,

    /** Temporary failure and attempts left: try again later. */
    RETRY,

    /** Nothing to check (alarm off, or this day was already checked). */
    SKIPPED,
}

/**
 * The traffic check itself: ask for today's travel time, run the pure [calculateAlarm], store
 * the result and reschedule. The WorkManager worker is a thin wrapper around [run].
 * No Android imports: tested with fakes.
 */
class TrafficCheck(
    private val repository: AlarmRepository,
    private val traffic: TrafficSource,
    private val alarmSync: AlarmSync,
    private val limits: AlarmLimits = AlarmLimits(),
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    suspend fun run(isLastAttempt: Boolean): CheckOutcome {
        val settings = repository.get() ?: return CheckOutcome.SKIPPED
        if (!settings.enabled) return CheckOutcome.SKIPPED

        val now = now()
        val plan = planAlarm(settings, repository.getAdjustment(), now, limits)
        // Already checked, or this work is left over from an earlier day and it's too early for the next one.
        val checkAt = plan.trafficCheck ?: return CheckOutcome.SKIPPED
        if (now.isBefore(checkAt - EARLY_TOLERANCE)) return CheckOutcome.SKIPPED

        // Ask for traffic at the moment the user would normally leave: wake up + preparation.
        // A departure time in the future gives Google's *predicted* traffic for that moment.
        val departure = maxOf(plan.occurrence + settings.preparation, now.plusMinutes(1))
        val result = traffic.travelTime(settings.origin, settings.destination, departure.toInstant())
        if (result is TrafficResult.Unavailable && result.retry && !isLastAttempt) return CheckOutcome.RETRY

        val todayTravel = (result as? TrafficResult.Success)?.travelTime
        val decision = calculateAlarm(
            AlarmInput(settings.arrival, settings.preparation, settings.usualTravel, todayTravel),
            limits,
        )
        var shift = signedShift(Duration.between(settings.originalAlarm, decision.alarm))
        // Late check (phone was offline, worker delayed): never schedule a ring in the past, ring now.
        val earliestRing = now.plusMinutes(1)
        if ((plan.occurrence + shift).isBefore(earliestRing)) shift = Duration.between(plan.occurrence, earliestRing)

        repository.saveAdjustment(
            TrafficAdjustment(plan.occurrence.toLocalDate(), shift, todayTravel, decision.reason, now.toInstant()),
        )
        alarmSync.sync()
        return CheckOutcome.DONE
    }

    private companion object {
        /** An exact alarm can be delivered a little early; don't treat that as "too early". */
        val EARLY_TOLERANCE: Duration = Duration.ofMinutes(1)
        val HALF_DAY: Duration = Duration.ofHours(12)
        val DAY: Duration = Duration.ofDays(1)

        /** 06:50 → 23:55 across midnight is −55 min, not +17h05. */
        fun signedShift(raw: Duration): Duration = when {
            raw > HALF_DAY -> raw - DAY
            raw < HALF_DAY.negated() -> raw + DAY
            else -> raw
        }
    }
}
