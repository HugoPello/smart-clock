package com.hugopello.smartclock.traffic

import java.time.Duration
import java.time.Instant

/** Where today's travel time comes from. The app uses TomTom; tests use a fake. */
interface TrafficSource {
    suspend fun travelTime(origin: String, destination: String, departure: Instant): TrafficResult
}

sealed interface TrafficResult {
    data class Success(val travelTime: Duration) : TrafficResult

    /**
     * No usable answer.
     * @property retry `true` for temporary problems (no network, server error, rate limit), worth trying
     * again in a moment. `false` when retrying won't help (no API key, bad request, no route found).
     */
    data class Unavailable(val retry: Boolean) : TrafficResult
}
