package com.hugopello.smartclock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hugopello.smartclock.MainActivity
import java.time.ZonedDateTime

/**
 * Real [AlarmScheduler] on top of [AlarmManager]. Two independent alarms, each with its own
 * [PendingIntent] (different action + request code), so scheduling one never replaces the other:
 * - ring: [AlarmManager.setAlarmClock], exact, wakes the phone in Doze, shown to the user as "next alarm";
 * - traffic check: [AlarmManager.setExactAndAllowWhileIdle], exact and works in Doze, but invisible.
 *   It also gives the app a few seconds of network access while the phone is dozing.
 */
class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun scheduleRing(at: ZonedDateTime): Boolean {
        // Tapping the alarm icon in the status bar opens the app.
        val info = AlarmManager.AlarmClockInfo(at.toInstant().toEpochMilli(), showAppIntent())
        return try {
            alarmManager.setAlarmClock(info, receiverIntent(AlarmReceiver.ACTION_RING, RING_REQUEST))
            true
        } catch (_: SecurityException) {
            // Android 12+: exact-alarm permission not granted (or revoked by the user).
            false
        }
    }

    override fun scheduleTrafficCheck(at: ZonedDateTime): Boolean = try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            at.toInstant().toEpochMilli(),
            receiverIntent(AlarmReceiver.ACTION_CHECK_TRAFFIC, CHECK_REQUEST),
        )
        true
    } catch (_: SecurityException) {
        false
    }

    override fun cancelRing() = alarmManager.cancel(receiverIntent(AlarmReceiver.ACTION_RING, RING_REQUEST))

    override fun cancelTrafficCheck() =
        alarmManager.cancel(receiverIntent(AlarmReceiver.ACTION_CHECK_TRAFFIC, CHECK_REQUEST))

    private fun receiverIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, AlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun showAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val RING_REQUEST = 0
        const val CHECK_REQUEST = 2 // 1 is the notification's Dismiss action
    }
}
