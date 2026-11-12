package com.hugopello.smartclock.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hugopello.smartclock.R

/**
 * The ringing itself is a notification:
 * - its channel plays the system alarm sound on the alarm audio stream (alarm volume, not ringer);
 * - [Notification.FLAG_INSISTENT] loops the sound until the notification is cancelled;
 * - its full-screen intent opens [RingingActivity] over the lock screen (or shows a heads-up
 *   notification if the phone is in use).
 * Dismissing = cancelling the notification, which also stops the sound.
 */
class AlarmNotifier(private val context: Context) {

    /** Safe to call on every app start. A channel's sound can't be changed after creation (only the user can). */
    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_alarm),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            val audio = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audio)
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showRinging() {
        // Android 13+: without POST_NOTIFICATIONS the notification (and so the sound) is dropped.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val ringingScreen = PendingIntent.getActivity(
            context,
            0,
            Intent(context, RingingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val dismiss = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.alarm_ringing_title))
            .setContentText(context.getString(R.string.alarm_ringing_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(ringingScreen, true)
            .setContentIntent(ringingScreen)
            .addAction(0, context.getString(R.string.dismiss), dismiss)
            // Don't ring forever if nobody is there (same default as the stock clock app).
            .setTimeoutAfter(RING_TIMEOUT_MS)
            .build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun dismiss() = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    private companion object {
        const val CHANNEL_ID = "alarm"
        const val NOTIFICATION_ID = 1
        const val RING_TIMEOUT_MS = 10 * 60 * 1000L
    }
}
