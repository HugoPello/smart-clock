package com.hugopello.smartclock.ui.alarm

import com.hugopello.smartclock.MainDispatcherRule
import com.hugopello.smartclock.data.AlarmRepository
import com.hugopello.smartclock.domain.AlarmSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.LocalTime

private class FakeAlarmRepository(var stored: AlarmSettings? = null) : AlarmRepository {
    override suspend fun get() = stored
    override suspend fun save(settings: AlarmSettings) {
        stored = settings
    }
}

class AlarmViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val saved = AlarmSettings(
        arrival = LocalTime.of(8, 30),
        preparation = Duration.ofMinutes(45),
        usualTravel = Duration.ofMinutes(20),
        destination = "Office",
    )

    @Test
    fun `loads saved settings on start`() {
        val vm = AlarmViewModel(FakeAlarmRepository(saved))

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(saved, state.settings)
        assertEquals(LocalTime.of(7, 25), state.wakeUpTime)
    }

    @Test
    fun `uses defaults when nothing saved`() {
        val vm = AlarmViewModel(FakeAlarmRepository())

        assertEquals(AlarmSettings.DEFAULT, vm.uiState.value.settings)
    }

    @Test
    fun `editing updates wake up time and marks unsaved`() {
        val vm = AlarmViewModel(FakeAlarmRepository(saved))

        vm.onPreparationChange(15)

        assertEquals(LocalTime.of(7, 10), vm.uiState.value.wakeUpTime)
        assertTrue(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `minutes are clamped to valid range`() {
        val vm = AlarmViewModel(FakeAlarmRepository(saved))

        vm.onPreparationChange(-1000)
        vm.onUsualTravelChange(-1000)

        assertEquals(Duration.ZERO, vm.uiState.value.settings.preparation)
        assertEquals(Duration.ofMinutes(1), vm.uiState.value.settings.usualTravel)
    }

    @Test
    fun `save persists trimmed settings and clears unsaved flag`() {
        val repo = FakeAlarmRepository()
        val vm = AlarmViewModel(repo)

        vm.onDestinationChange("  Office  ")
        vm.onArrivalChange(LocalTime.of(10, 0))
        vm.save()

        assertEquals("Office", repo.stored?.destination)
        assertEquals(LocalTime.of(10, 0), repo.stored?.arrival)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `cannot save without destination`() {
        val repo = FakeAlarmRepository()
        val vm = AlarmViewModel(repo)

        vm.onDestinationChange("   ")
        vm.save()

        assertFalse(vm.uiState.value.canSave)
        assertNull(repo.stored)
    }
}
