package com.hugopello.smartclock.alarm

import com.hugopello.smartclock.data.AlarmRepository
import com.hugopello.smartclock.domain.AlarmLimits
import com.hugopello.smartclock.domain.planAlarm
import java.time.ZonedDateTime

/** Something that can register exact alarms with the system. Faked in unit tests. */
interface AlarmScheduler {
    /** The alarm the user hears. Replaces any previous one. `false` if exact alarms aren't allowed. */
    fun scheduleRing(at: ZonedDateTime): Boolean

    /** Silent alarm that starts the traffic check. Replaces any previous one. */
    fun scheduleTrafficCheck(at: ZonedDateTime): Boolean

    fun cancelRing()

    fun cancelTrafficCheck()
}

sealed interface SyncResult {
    /** @property trafficCheckAt when traffic will be checked; `null` if it already was for this alarm. */
    data class Scheduled(val at: ZonedDateTime, val trafficCheckAt: ZonedDateTime?) : SyncResult

    /** No settings saved yet, or the alarm is switched off. */
    data object Disabled : SyncResult

    /** The alarm is on but the exact-alarm permission is missing. */
    data object MissingPermission : SyncResult
}

/**
 * Single source of truth for "what should be scheduled right now".
 * Every trigger (save, app start, boot, time change, after ringing, permission granted, traffic checked)
 * calls [sync] instead of having its own scheduling logic. Calling it twice is harmless.
 *
 * The ring is always registered, at the original time until traffic says otherwise: if the
 * check never happens or fails, the alarm still rings at the original time.
 *
 * No Android imports: unit-tested with a fake [AlarmScheduler] and a fixed clock.
 */
class AlarmSync(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val limits: AlarmLimits = AlarmLimits(),
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    /**
     * @param justRang `true` when called from the alarm itself. Planning starts one minute later,
     * so an alarm delivered a few ms early can't be rescheduled for the same minute.
     */
    suspend fun sync(justRang: Boolean = false): SyncResult {
        val settings = repository.get()
        if (settings == null || !settings.enabled) {
            scheduler.cancelRing()
            scheduler.cancelTrafficCheck()
            return SyncResult.Disabled
        }
        val from = if (justRang) now().plusMinutes(1) else now()
        val plan = planAlarm(settings, repository.getAdjustment(), from, limits)

        if (!scheduler.scheduleRing(plan.ring)) return SyncResult.MissingPermission
        // A check time in the past (alarm saved late, phone was off...) fires right away: still useful.
        plan.trafficCheck?.let(scheduler::scheduleTrafficCheck) ?: scheduler.cancelTrafficCheck()
        return SyncResult.Scheduled(plan.ring, plan.trafficCheck)
    }
}
