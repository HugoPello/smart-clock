package com.hugopello.smartclock.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hugopello.smartclock.R
import com.hugopello.smartclock.SmartClockApp
import com.hugopello.smartclock.ui.theme.SmartclockTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Full-screen "alarm ringing" screen, shown over the lock screen by the notification's full-screen intent. */
class RingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        setContent {
            SmartclockTheme {
                RingingScreen(time = time, onDismiss = ::dismiss)
            }
        }
    }

    private fun dismiss() {
        (application as SmartClockApp).alarmNotifier.dismiss()
        finish()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }
}

@Composable
private fun RingingScreen(time: String, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            Modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(time, style = MaterialTheme.typography.displayLarge)
            Text(stringResource(R.string.alarm_ringing_title), style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(top = 48.dp)
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text(stringResource(R.string.dismiss), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Preview
@Composable
private fun RingingScreenPreview() {
    SmartclockTheme { RingingScreen(time = "07:00", onDismiss = {}) }
}
