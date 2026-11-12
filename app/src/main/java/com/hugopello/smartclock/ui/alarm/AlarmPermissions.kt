package com.hugopello.smartclock.ui.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * The three things the alarm needs from the user. Each one only exists from a certain Android
 * version; on older versions it is granted automatically, so it counts as `true`.
 */
data class AlarmPermissions(
    /** Android 12+ ("Alarms & reminders" special access). Without it [AlarmManager.setAlarmClock] throws. */
    val exactAlarm: Boolean,
    /** Android 13+ runtime permission. Without it the ringing notification is never shown. */
    val notifications: Boolean,
    /** Android 14+ special access. Without it the alarm still sounds, but as a heads-up, not full screen. */
    val fullScreen: Boolean,
) {
    companion object {
        fun check(context: Context) = AlarmPermissions(
            exactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
            notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
            fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent(),
        )

        /*
         * Special accesses can't be requested with a dialog: the user must flip a switch in Settings.
         * Each intent opens the exact screen for this app.
         */

        fun exactAlarmSettings(context: Context): Intent? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))
            } else {
                null
            }

        fun fullScreenSettings(context: Context): Intent? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context))
            } else {
                null
            }

        /** Fallback once the runtime dialog was denied: Android stops showing it after two denials. */
        fun notificationSettings(context: Context): Intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

        private fun packageUri(context: Context) = "package:${context.packageName}".toUri()
    }
}
