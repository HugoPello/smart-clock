package com.hugopello.smartclock.ui.alarm

import com.hugopello.smartclock.FakeAlarmRepository
import com.hugopello.smartclock.FakeAlarmScheduler
import com.hugopello.smartclock.MainDispatcherRule
import com.hugopello.smartclock.alarm.AlarmSync
import com.hugopello.smartclock.alarm.SyncResult
import com.hugopello.smartclock.domain.AlarmSettings
import com.hugopello.smartclock.domain.DecisionReason
import com.hugopello.smartclock.domain.TrafficAdjustment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class AlarmViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val saved = AlarmSettings(
        arrival = LocalTime.of(8, 30),
        preparation = Duration.ofMinutes(45),
        usualTravel = Duration.ofMinutes(20),
        destination = "Office",
        origin = "Home",
    )

    private val scheduler = FakeAlarmScheduler()

    private fun viewModel(repo: FakeAlarmRepository) = AlarmViewModel(repo, AlarmSync(repo, scheduler))

    @Test
    fun `loads saved settings on start`() {
        val vm = viewModel(FakeAlarmRepository(saved))

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(saved, state.settings)
        assertEquals(LocalTime.of(7, 25), state.wakeUpTime)
    }

    @Test
    fun `uses defaults when nothing saved`() {
        val vm = viewModel(FakeAlarmRepository())

        assertEquals(AlarmSettings.DEFAULT, vm.uiState.value.settings)
    }

    @Test
    fun `editing updates wake up time and marks unsaved`() {
        val vm = viewModel(FakeAlarmRepository(saved))

        vm.onPreparationChange(15)

        assertEquals(LocalTime.of(7, 10), vm.uiState.value.wakeUpTime)
        assertTrue(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `minutes are clamped to valid range`() {
        val vm = viewModel(FakeAlarmRepository(saved))

        vm.onPreparationChange(-1000)
        vm.onUsualTravelChange(-1000)

        assertEquals(Duration.ZERO, vm.uiState.value.settings.preparation)
        assertEquals(Duration.ofMinutes(1), vm.uiState.value.settings.usualTravel)
    }

    @Test
    fun `save persists trimmed settings and clears unsaved flag`() {
        val repo = FakeAlarmRepository()
        val vm = viewModel(repo)

        vm.onOriginChange("  Home  ")
        vm.onDestinationChange("  Office  ")
        vm.onArrivalChange(LocalTime.of(10, 0))
        vm.save()

        assertEquals("Home", repo.stored?.origin)
        assertEquals("Office", repo.stored?.destination)
        assertEquals(LocalTime.of(10, 0), repo.stored?.arrival)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `cannot save without destination`() {
        val repo = FakeAlarmRepository()
        val vm = viewModel(repo)

        vm.onDestinationChange("   ")
        vm.save()

        assertFalse(vm.uiState.value.canSave)
        assertNull(repo.stored)
    }

    @Test
    fun `saving an enabled alarm schedules it`() {
        val repo = FakeAlarmRepository(saved)
        val vm = viewModel(repo)

        vm.onEnabledChange(true)
        vm.save()

        assertEquals(true, repo.stored?.enabled)
        assertEquals(LocalTime.of(7, 25), scheduler.ringAt?.toLocalTime())
        assertTrue(vm.uiState.value.schedule is SyncResult.Scheduled)
    }

    @Test
    fun `switching the alarm off cancels it`() {
        val repo = FakeAlarmRepository(saved.copy(enabled = true))
        val vm = viewModel(repo)
        vm.refreshSchedule()

        vm.onEnabledChange(false)
        vm.save()

        assertNull(scheduler.ringAt)
        assertEquals(SyncResult.Disabled, vm.uiState.value.schedule)
    }

    @Test
    fun `traffic result is hidden while editing`() {
        val adjustment = TrafficAdjustment(
            LocalDate.now(),
            Duration.ofMinutes(-10),
            Duration.ofMinutes(30),
            DecisionReason.ADJUSTED,
            Instant.EPOCH,
        )
        val state = AlarmUiState(
            isLoading = false,
            schedule = SyncResult.Scheduled(ZonedDateTime.now(), trafficCheckAt = null),
            adjustment = adjustment,
        )

        assertEquals(adjustment, state.appliedTraffic)
        assertNull(state.copy(hasUnsavedChanges = true).appliedTraffic)
    }

    @Test
    fun `refresh reports missing permission`() {
        scheduler.allowed = false
        val vm = viewModel(FakeAlarmRepository(saved.copy(enabled = true)))

        vm.refreshSchedule()

        assertEquals(SyncResult.MissingPermission, vm.uiState.value.schedule)
    }

    @Test
    fun `cannot save without starting point`() {
        val repo = FakeAlarmRepository()
        val vm = viewModel(repo)

        vm.onDestinationChange("Office")
        vm.save()

        assertFalse(vm.uiState.value.canSave)
        assertNull(repo.stored)
    }
}
