package com.hugopello.smartclock.alarm

import com.hugopello.smartclock.FakeAlarmRepository
import com.hugopello.smartclock.FakeAlarmScheduler
import com.hugopello.smartclock.domain.AlarmSettings
import com.hugopello.smartclock.domain.DecisionReason
import com.hugopello.smartclock.domain.TrafficAdjustment
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmSyncTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), zone)

    // 9:00 − 30 − 30 → rings at 08:00; traffic check at 08:00 − 30 − 15 = 07:15
    private val enabled = AlarmSettings(
        arrival = LocalTime.of(9, 0),
        preparation = Duration.ofMinutes(30),
        usualTravel = Duration.ofMinutes(30),
        destination = "Office",
        enabled = true,
        origin = "Home",
    )

    private val scheduler = FakeAlarmScheduler()

    private fun sync(settings: AlarmSettings?, now: String, adjustment: TrafficAdjustment? = null) =
        AlarmSync(FakeAlarmRepository(settings, adjustment), scheduler, now = { at(now) })

    @Test
    fun `enabled alarm rings at its original time and schedules the traffic check`() = runTest {
        val result = sync(enabled, now = "2026-11-12T06:00").sync()

        assertEquals(SyncResult.Scheduled(at("2026-11-12T08:00"), at("2026-11-12T07:15")), result)
        assertEquals(at("2026-11-12T08:00"), scheduler.ringAt)
        assertEquals(at("2026-11-12T07:15"), scheduler.trafficCheckAt)
    }

    @Test
    fun `stored adjustment moves the ring and cancels the check`() = runTest {
        scheduler.scheduleTrafficCheck(at("2026-11-12T07:15"))
        val adjustment = TrafficAdjustment(
            LocalDate.parse("2026-11-12"),
            Duration.ofMinutes(-10),
            Duration.ofMinutes(40),
            DecisionReason.ADJUSTED,
            Instant.EPOCH,
        )

        val result = sync(enabled, now = "2026-11-12T07:20", adjustment).sync()

        assertEquals(SyncResult.Scheduled(at("2026-11-12T07:50"), null), result)
        assertNull(scheduler.trafficCheckAt)
    }

    @Test
    fun `disabled alarm cancels ring and check`() = runTest {
        scheduler.scheduleRing(at("2026-11-12T08:00"))
        scheduler.scheduleTrafficCheck(at("2026-11-12T07:15"))

        val result = sync(enabled.copy(enabled = false), now = "2026-11-12T06:00").sync()

        assertEquals(SyncResult.Disabled, result)
        assertNull(scheduler.ringAt)
        assertNull(scheduler.trafficCheckAt)
    }

    @Test
    fun `nothing saved yet counts as disabled`() = runTest {
        val result = sync(null, now = "2026-11-12T06:00").sync()

        assertEquals(SyncResult.Disabled, result)
    }

    @Test
    fun `missing permission is reported, not thrown`() = runTest {
        scheduler.allowed = false

        val result = sync(enabled, now = "2026-11-12T06:00").sync()

        assertEquals(SyncResult.MissingPermission, result)
    }

    @Test
    fun `after ringing a few ms early, next alarm is tomorrow`() = runTest {
        val result = sync(enabled, now = "2026-11-12T07:59:59.990").sync(justRang = true)

        assertEquals(SyncResult.Scheduled(at("2026-11-13T08:00"), at("2026-11-13T07:15")), result)
    }
}
