package com.hugopello.smartclock.ui.alarm

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hugopello.smartclock.R
import com.hugopello.smartclock.ui.theme.SmartclockTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val STEP_MINUTES = 5L

/** Stateful entry point: gets the ViewModel and wires events. */
@Composable
fun AlarmRoute(
    modifier: Modifier = Modifier,
    viewModel: AlarmViewModel = viewModel(factory = AlarmViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AlarmScreen(
        state = state,
        onArrivalChange = viewModel::onArrivalChange,
        onPreparationChange = viewModel::onPreparationChange,
        onUsualTravelChange = viewModel::onUsualTravelChange,
        onDestinationChange = viewModel::onDestinationChange,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

/** Stateless: only draws [state] and reports events. Easy to preview and test. */
@Composable
fun AlarmScreen(
    state: AlarmUiState,
    onArrivalChange: (LocalTime) -> Unit,
    onPreparationChange: (Long) -> Unit,
    onUsualTravelChange: (Long) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSave: () -> Unit,
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
            }
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
            state = AlarmUiState(isLoading = false, hasUnsavedChanges = true),
            onArrivalChange = {},
            onPreparationChange = {},
            onUsualTravelChange = {},
            onDestinationChange = {},
            onSave = {},
        )
    }
}
