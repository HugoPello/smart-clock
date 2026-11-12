package com.hugopello.smartclock.alarm

import com.hugopello.smartclock.data.AlarmRepository
import com.hugopello.smartclock.domain.nextOccurrence
import java.time.ZonedDateTime

/** Something that can register one exact alarm with the system. Faked in unit tests. */
interface AlarmScheduler {
    /** Replaces any previous alarm. Returns `false` if the system doesn't allow exact alarms. */
    fun schedule(at: ZonedDateTime): Boolean

    fun cancel()
}

sealed interface SyncResult {
    data class Scheduled(val at: ZonedDateTime) : SyncResult

    /** No settings saved yet, or the alarm is switched off. */
    data object Disabled : SyncResult

    /** The alarm is on but the exact-alarm permission is missing. */
    data object MissingPermission : SyncResult
}

/**
 * Single source of truth for "what should be scheduled right now".
 * Every trigger (save, app start, boot, time change, after ringing, permission granted)
 * calls [sync] instead of having its own scheduling logic. Calling it twice is harmless.
 *
 * No Android imports: unit-tested with a fake [AlarmScheduler] and a fixed clock.
 */
class AlarmSync(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    /**
     * @param justRang `true` when called from the alarm itself. The search for the next ring starts
     * one minute later, so an alarm delivered a few ms early can't be rescheduled for the same minute.
     */
    suspend fun sync(justRang: Boolean = false): SyncResult {
        val settings = repository.get()
        if (settings == null || !settings.enabled) {
            scheduler.cancel()
            return SyncResult.Disabled
        }
        val from = if (justRang) now().plusMinutes(1) else now()
        val at = nextOccurrence(settings.originalAlarm, from)
        return if (scheduler.schedule(at)) SyncResult.Scheduled(at) else SyncResult.MissingPermission
    }
}
