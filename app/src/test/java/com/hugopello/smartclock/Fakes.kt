package com.hugopello.smartclock

import com.hugopello.smartclock.alarm.AlarmScheduler
import com.hugopello.smartclock.data.AlarmRepository
import com.hugopello.smartclock.domain.AlarmSettings
import com.hugopello.smartclock.domain.TrafficAdjustment
import com.hugopello.smartclock.traffic.TrafficResult
import com.hugopello.smartclock.traffic.TrafficSource
import java.time.Instant
import java.time.ZonedDateTime

/** In-memory repository: what the ViewModel, AlarmSync and TrafficCheck see in tests instead of Room. */
class FakeAlarmRepository(
    var stored: AlarmSettings? = null,
    var adjustment: TrafficAdjustment? = null,
) : AlarmRepository {
    override suspend fun get() = stored
    override suspend fun save(settings: AlarmSettings) {
        stored = settings
        adjustment = null // same contract as the Room implementation
    }

    override suspend fun getAdjustment() = adjustment
    override suspend fun saveAdjustment(adjustment: TrafficAdjustment) {
        this.adjustment = adjustment
    }
}

/** Records what would have been registered with AlarmManager. */
class FakeAlarmScheduler(var allowed: Boolean = true) : AlarmScheduler {
    var ringAt: ZonedDateTime? = null
        private set
    var trafficCheckAt: ZonedDateTime? = null
        private set

    override fun scheduleRing(at: ZonedDateTime): Boolean {
        if (allowed) ringAt = at
        return allowed
    }

    override fun scheduleTrafficCheck(at: ZonedDateTime): Boolean {
        if (allowed) trafficCheckAt = at
        return allowed
    }

    override fun cancelRing() {
        ringAt = null
    }

    override fun cancelTrafficCheck() {
        trafficCheckAt = null
    }
}

/** Answers with [result] and remembers what it was asked. */
class FakeTrafficSource(var result: TrafficResult) : TrafficSource {
    var calls = 0
        private set
    var lastDeparture: Instant? = null
        private set

    override suspend fun travelTime(origin: String, destination: String, departure: Instant): TrafficResult {
        calls++
        lastDeparture = departure
        return result
    }
}
