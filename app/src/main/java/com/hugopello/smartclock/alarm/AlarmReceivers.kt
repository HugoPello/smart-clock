package com.hugopello.smartclock.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hugopello.smartclock.SmartClockApp
import com.hugopello.smartclock.traffic.TrafficCheckWorker
import kotlinx.coroutines.launch

/**
 * Receives the [AlarmManager] alarms (ring, traffic check) and the "Dismiss" button of the notification.
 * Not exported: only our own PendingIntents can reach it.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SmartClockApp
        when (intent.action) {
            ACTION_RING -> {
                app.alarmNotifier.showRinging()
                // The alarm is daily: schedule tomorrow's.
                runAsync(app) { app.alarmSync.sync(justRang = true) }
            }
            ACTION_DISMISS -> app.alarmNotifier.dismiss()
            // Hand over to WorkManager: network constraint + retries. Enqueuing is instant, no goAsync needed.
            ACTION_CHECK_TRAFFIC -> TrafficCheckWorker.enqueue(context)
        }
    }

    companion object {
        const val ACTION_RING = "com.hugopello.smartclock.action.RING"
        const val ACTION_DISMISS = "com.hugopello.smartclock.action.DISMISS"
        const val ACTION_CHECK_TRAFFIC = "com.hugopello.smartclock.action.CHECK_TRAFFIC"
    }
}

/**
 * Situations where the system forgets or misplaces our alarm:
 * - reboot: [AlarmManager] keeps alarms only in memory;
 * - app update: re-register to be safe;
 * - clock or time zone changed: the alarm is stored as an absolute instant, so "07:00" must be recomputed;
 * - exact-alarm permission just granted: there may be nothing scheduled yet.
 *
 * Not exported: these broadcasts come from the system, which can reach non-exported receivers.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val app = context.applicationContext as SmartClockApp
        runAsync(app) { app.alarmSync.sync() }
    }

    private companion object {
        // InlinedApi: the API 31 constant is just a String copied into our code. On older Android
        // it is never broadcast, so comparing against it is harmless.
        @SuppressLint("InlinedApi")
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}

/**
 * `onReceive` runs on the main thread and the process may be killed as soon as it returns.
 * [BroadcastReceiver.goAsync] keeps the broadcast "alive" (up to ~10 s) until we call `finish()`,
 * so the database read can run in a coroutine.
 */
private fun BroadcastReceiver.runAsync(app: SmartClockApp, block: suspend () -> Unit) {
    val pending = goAsync()
    app.appScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}
