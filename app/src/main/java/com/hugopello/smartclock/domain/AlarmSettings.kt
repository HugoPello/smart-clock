package com.hugopello.smartclock.domain

import java.time.Duration
import java.time.LocalTime

/**
 * What the user configures. Domain model: no Room/Android annotations.
 * @property enabled whether the alarm should actually be scheduled (off by default).
 * @property origin where the trip starts (an address, e.g. home). Traffic is checked from here to [destination].
 */
data class AlarmSettings(
    val arrival: LocalTime,
    val preparation: Duration,
    val usualTravel: Duration,
    val destination: String,
    val enabled: Boolean = false,
    val origin: String = "",
) {
    /** Alarm time with no traffic data (the "original" alarm). */
    val originalAlarm: LocalTime
        get() = calculateAlarm(
            AlarmInput(arrival, preparation, usualTravel, todayTravel = null),
        ).alarm

    companion object {
        val DEFAULT = AlarmSettings(
            arrival = LocalTime.of(9, 0),
            preparation = Duration.ofMinutes(30),
            usualTravel = Duration.ofMinutes(30),
            destination = "",
        )
    }
}
