package com.hugopello.smartclock.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hugopello.smartclock.SmartClockApp
import com.hugopello.smartclock.alarm.AlarmSync
import com.hugopello.smartclock.alarm.SyncResult
import com.hugopello.smartclock.data.AlarmRepository
import com.hugopello.smartclock.domain.AlarmSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime

data class AlarmUiState(
    val isLoading: Boolean = true,
    val settings: AlarmSettings = AlarmSettings.DEFAULT,
    val hasUnsavedChanges: Boolean = false,
    /** What is actually registered with the system (from the last [AlarmSync.sync]); `null` until known. */
    val schedule: SyncResult? = null,
) {
    val wakeUpTime: LocalTime get() = settings.originalAlarm
    val canSave: Boolean get() = !isLoading && hasUnsavedChanges && settings.destination.isNotBlank()
}

class AlarmViewModel(
    private val repository: AlarmRepository,
    private val alarmSync: AlarmSync,
) : ViewModel() {

    // Private mutable state, public read-only view: only the ViewModel can change it.
    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.get()
            _uiState.update { it.copy(isLoading = false, settings = saved ?: it.settings) }
        }
    }

    fun onArrivalChange(time: LocalTime) = edit { it.copy(arrival = time) }

    fun onPreparationChange(deltaMinutes: Long) = edit {
        it.copy(preparation = it.preparation.plusMinutesIn(deltaMinutes, PREPARATION_RANGE))
    }

    fun onUsualTravelChange(deltaMinutes: Long) = edit {
        it.copy(usualTravel = it.usualTravel.plusMinutesIn(deltaMinutes, TRAVEL_RANGE))
    }

    fun onDestinationChange(text: String) = edit { it.copy(destination = text) }

    fun onEnabledChange(enabled: Boolean) = edit { it.copy(enabled = enabled) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            repository.save(state.settings.copy(destination = state.settings.destination.trim()))
            val schedule = alarmSync.sync()
            _uiState.update { it.copy(hasUnsavedChanges = false, schedule = schedule) }
        }
    }

    /**
     * Called when the screen comes back to the foreground: the user may have just granted
     * the exact-alarm permission in Settings, so try to schedule again.
     */
    fun refreshSchedule() {
        viewModelScope.launch {
            val schedule = alarmSync.sync()
            _uiState.update { it.copy(schedule = schedule) }
        }
    }

    private fun edit(change: (AlarmSettings) -> AlarmSettings) {
        _uiState.update { it.copy(settings = change(it.settings), hasUnsavedChanges = true) }
    }

    companion object {
        val PREPARATION_RANGE = 0L..180L
        val TRAVEL_RANGE = 1L..240L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SmartClockApp
                AlarmViewModel(app.alarmRepository, app.alarmSync)
            }
        }
    }
}

private fun Duration.plusMinutesIn(delta: Long, range: LongRange): Duration =
    Duration.ofMinutes((toMinutes() + delta).coerceIn(range))
