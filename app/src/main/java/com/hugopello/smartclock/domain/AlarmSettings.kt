package com.hugopello.smartclock.domain

import java.time.Duration
import java.time.LocalTime

/** What the user configures. Domain model: no Room/Android annotations. */
data class AlarmSettings(
    val arrival: LocalTime,
    val preparation: Duration,
    val usualTravel: Duration,
    val destination: String,
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
