package com.hugopello.smartclock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hugopello.smartclock.MainActivity
import java.time.ZonedDateTime

/**
 * Real [AlarmScheduler] on top of [AlarmManager.setAlarmClock]:
 * - exact and allowed to fire in Doze (unlike `set`/`setExact`, and unlike WorkManager);
 * - the system shows it as the "next alarm" (status-bar icon, lock screen).
 *
 * Always the same [PendingIntent] (same request code + same intent) → scheduling again replaces it,
 * and [cancel] finds it.
 */
class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(at: ZonedDateTime): Boolean {
        // Tapping the alarm icon in the status bar opens the app.
        val info = AlarmManager.AlarmClockInfo(at.toInstant().toEpochMilli(), showAppIntent())
        return try {
            alarmManager.setAlarmClock(info, ringIntent())
            true
        } catch (_: SecurityException) {
            // Android 12+: exact-alarm permission not granted (or revoked by the user).
            false
        }
    }

    override fun cancel() = alarmManager.cancel(ringIntent())

    private fun ringIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_RING),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun showAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
