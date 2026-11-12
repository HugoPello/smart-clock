package com.hugopello.smartclock

import android.app.Application
import com.hugopello.smartclock.data.AlarmRepository
import com.hugopello.smartclock.data.AppDatabase
import com.hugopello.smartclock.data.RoomAlarmRepository

/**
 * Manual dependency injection: one place that builds the long-lived objects.
 * Enough for an app this size; Hilt would add a lot of setup for a single repository.
 */
class SmartClockApp : Application() {
    val alarmRepository: AlarmRepository by lazy {
        RoomAlarmRepository(AppDatabase.create(this).alarmDao())
    }
}
