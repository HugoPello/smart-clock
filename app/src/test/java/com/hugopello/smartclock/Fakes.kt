package com.hugopello.smartclock

import com.hugopello.smartclock.alarm.AlarmScheduler
import com.hugopello.smartclock.data.AlarmRepository
import com.hugopello.smartclock.domain.AlarmSettings
import java.time.ZonedDateTime

/** In-memory repository: what the ViewModel and AlarmSync see in tests instead of Room. */
class FakeAlarmRepository(var stored: AlarmSettings? = null) : AlarmRepository {
    override suspend fun get() = stored
    override suspend fun save(settings: AlarmSettings) {
        stored = settings
    }
}

/** Records what would have been registered with AlarmManager. */
class FakeAlarmScheduler(var allowed: Boolean = true) : AlarmScheduler {
    var scheduledAt: ZonedDateTime? = null
        private set

    override fun schedule(at: ZonedDateTime): Boolean {
        if (allowed) scheduledAt = at
        return allowed
    }

    override fun cancel() {
        scheduledAt = null
    }
}
