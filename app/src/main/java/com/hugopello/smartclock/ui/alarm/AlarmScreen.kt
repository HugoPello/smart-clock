package com.hugopello.smartclock.ui.alarm

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hugopello.smartclock.R
import com.hugopello.smartclock.alarm.SyncResult
import com.hugopello.smartclock.domain.AlarmSettings
import com.hugopello.smartclock.domain.TrafficAdjustment
import com.hugopello.smartclock.ui.theme.SmartclockTheme
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val NEXT_ALARM_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm")
private const val STEP_MINUTES = 5L

/** Stateful entry point: gets the ViewModel, checks permissions and wires events. */
@Composable
fun AlarmRoute(
    modifier: Modifier = Modifier,
    viewModel: AlarmViewModel = viewModel(factory = AlarmViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permissions change outside the app (in Settings), so re-check every time the screen comes back.
    var permissions by remember { mutableStateOf(AlarmPermissions.check(context)) }
    LifecycleResumeEffect(Unit) {
        permissions = AlarmPermissions.check(context)
        viewModel.refreshSchedule()
        onPauseOrDispose { }
    }

    // First tap: system dialog. After that Android may refuse to show it again, so go to Settings.
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissions = AlarmPermissions.check(context)
    }

    AlarmScreen(
        state = state,
        permissions = permissions,
        onArrivalChange = viewModel::onArrivalChange,
        onPreparationChange = viewModel::onPreparationChange,
        onUsualTravelChange = viewModel::onUsualTravelChange,
        onOriginChange = viewModel::onOriginChange,
        onDestinationChange = viewModel::onDestinationChange,
        onEnabledChange = viewModel::onEnabledChange,
        onSave = viewModel::save,
        onAllowExactAlarm = { AlarmPermissions.exactAlarmSettings(context)?.let(context::startActivity) },
        onAllowNotifications = {
            if (!askedNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                askedNotifications = true
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(AlarmPermissions.notificationSettings(context))
            }
        },
        onAllowFullScreen = { AlarmPermissions.fullScreenSettings(context)?.let(context::startActivity) },
        modifier = modifier,
    )
}

/** Stateless: only draws [state] and reports events. Easy to preview and test. */
@Composable
fun AlarmScreen(
    state: AlarmUiState,
    permissions: AlarmPermissions,
    onArrivalChange: (LocalTime) -> Unit,
    onPreparationChange: (Long) -> Unit,
    onUsualTravelChange: (Long) -> Unit,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onAllowExactAlarm: () -> Unit,
    onAllowNotifications: () -> Unit,
    onAllowFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Column(modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val settings = state.settings

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.alarm_rings_at), style = MaterialTheme.typography.titleMedium)
                Text(state.wakeUpTime.format(TIME_FORMAT), style = MaterialTheme.typography.displayLarge)
                state.schedule?.let { ScheduleStatus(it) }
                state.appliedTraffic?.let { TrafficStatus(it, state.settings.usualTravel) }
            }
        }

        // Only ask for permissions when the alarm is (or is about to be) on.
        if (settings.enabled) {
            if (!permissions.exactAlarm) {
                PermissionCard(stringResource(R.string.permission_exact_alarm), onAllowExactAlarm)
            }
            if (!permissions.notifications) {
                PermissionCard(stringResource(R.string.permission_notifications), onAllowNotifications)
            }
            if (!permissions.fullScreen) {
                PermissionCard(stringResource(R.string.permission_full_screen), onAllowFullScreen)
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.alarm_enabled),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Switch(checked = settings.enabled, onCheckedChange = onEnabledChange)
        }

        OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.arrival_at, settings.arrival.format(TIME_FORMAT)))
        }

        MinutesStepper(
            label = stringResource(R.string.preparation),
            minutes = settings.preparation.toMinutes(),
            onChange = onPreparationChange,
        )
        MinutesStepper(
            label = stringResource(R.string.usual_travel),
            minutes = settings.usualTravel.toMinutes(),
            onChange = onUsualTravelChange,
        )

        OutlinedTextField(
            value = settings.origin,
            onValueChange = onOriginChange,
            label = { Text(stringResource(R.string.origin)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = settings.destination,
            onValueChange = onDestinationChange,
            label = { Text(stringResource(R.string.destination)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.hasUnsavedChanges) R.string.save else R.string.saved))
        }
    }

    if (showTimePicker) {
        ArrivalTimeDialog(
            initial = settings.arrival,
            onConfirm = {
                onArrivalChange(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun ScheduleStatus(schedule: SyncResult) {
    val text = when (schedule) {
        is SyncResult.Scheduled -> stringResource(R.string.schedule_next, schedule.at.format(NEXT_ALARM_FORMAT))
        SyncResult.Disabled -> stringResource(R.string.schedule_off)
        SyncResult.MissingPermission -> stringResource(R.string.schedule_missing_permission)
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
    val checkAt = (schedule as? SyncResult.Scheduled)?.trafficCheckAt
    if (checkAt != null) {
        Text(
            stringResource(R.string.traffic_check_at, checkAt.format(TIME_FORMAT)),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TrafficStatus(adjustment: TrafficAdjustment, usualTravel: Duration) {
    val travel = adjustment.todayTravel
    val shift = adjustment.shift.toMinutes()
    val text = when {
        travel == null -> stringResource(R.string.traffic_unavailable)
        shift < 0 -> stringResource(R.string.traffic_earlier, travel.toMinutes(), usualTravel.toMinutes(), -shift)
        shift > 0 -> stringResource(R.string.traffic_later, travel.toMinutes(), usualTravel.toMinutes(), shift)
        else -> stringResource(R.string.traffic_same, travel.toMinutes(), usualTravel.toMinutes())
    }
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun PermissionCard(text: String, onAllow: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAllow) { Text(stringResource(R.string.permission_grant)) }
        }
    }
}

@Composable
private fun MinutesStepper(label: String, minutes: Long, onChange: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        FilledTonalIconButton(onClick = { onChange(-STEP_MINUTES) }) { Text("−") }
        Text(
            stringResource(R.string.minutes, minutes),
            Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        FilledTonalIconButton(onClick = { onChange(STEP_MINUTES) }) { Text("+") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArrivalTimeDialog(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
        text = { TimePicker(pickerState) },
    )
}

@Preview(showBackground = true)
@Composable
private fun AlarmScreenPreview() {
    SmartclockTheme {
        AlarmScreen(
            state = AlarmUiState(
                isLoading = false,
                settings = AlarmSettings.DEFAULT.copy(enabled = true),
                hasUnsavedChanges = true,
            ),
            permissions = AlarmPermissions(exactAlarm = false, notifications = true, fullScreen = true),
            onArrivalChange = {},
            onPreparationChange = {},
            onUsualTravelChange = {},
            onOriginChange = {},
            onDestinationChange = {},
            onEnabledChange = {},
            onSave = {},
            onAllowExactAlarm = {},
            onAllowNotifications = {},
            onAllowFullScreen = {},
        )
    }
}
