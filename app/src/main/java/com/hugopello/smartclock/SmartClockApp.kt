package com.hugopello.smartclock

import android.app.Application
import com.hugopello.smartclock.alarm.AlarmNotifier
import com.hugopello.smartclock.alarm.AlarmSync
import com.hugopello.smartclock.alarm.AndroidAlarmScheduler
import com.hugopello.smartclock.data.AlarmRepository
import com.hugopello.smartclock.data.AppDatabase
import com.hugopello.smartclock.data.RoomAlarmRepository
import com.hugopello.smartclock.traffic.AppIdentityInterceptor
import com.hugopello.smartclock.traffic.GoogleRoutesTrafficSource
import com.hugopello.smartclock.traffic.RoutesApi
import com.hugopello.smartclock.traffic.TrafficCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency injection: one place that builds the long-lived objects.
 * Enough for an app this size; Hilt would add a lot of setup for a handful of classes.
 */
class SmartClockApp : Application() {

    /** Lives as long as the process. For work that must not die with a screen (receivers, startup sync). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val alarmRepository: AlarmRepository by lazy {
        RoomAlarmRepository(AppDatabase.create(this).alarmDao())
    }
    val alarmNotifier by lazy { AlarmNotifier(this) }
    val alarmSync by lazy { AlarmSync(alarmRepository, AndroidAlarmScheduler(this)) }
    val trafficCheck by lazy {
        val api = RoutesApi.create(interceptors = arrayOf(AppIdentityInterceptor(this)))
        TrafficCheck(alarmRepository, GoogleRoutesTrafficSource(api, BuildConfig.ROUTES_API_KEY), alarmSync)
    }

    override fun onCreate() {
        super.onCreate()
        alarmNotifier.createChannel()
        // A force-stop deletes our alarms and we get no broadcast about it: re-register on every start.
        appScope.launch { alarmSync.sync() }
    }
}
