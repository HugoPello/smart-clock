package com.hugopello.smartclock.alarm

import com.hugopello.smartclock.FakeAlarmRepository
import com.hugopello.smartclock.FakeAlarmScheduler
import com.hugopello.smartclock.domain.AlarmSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmSyncTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), zone)

    // 9:00 − 30 − 30 → rings at 08:00
    private val enabled = AlarmSettings(
        arrival = LocalTime.of(9, 0),
        preparation = Duration.ofMinutes(30),
        usualTravel = Duration.ofMinutes(30),
        destination = "Office",
        enabled = true,
    )

    private val scheduler = FakeAlarmScheduler()

    private fun sync(settings: AlarmSettings?, now: String) =
        AlarmSync(FakeAlarmRepository(settings), scheduler, now = { at(now) })

    @Test
    fun `enabled alarm is scheduled at its next occurrence`() = runTest {
        val result = sync(enabled, now = "2026-11-12T06:00").sync()

        assertEquals(SyncResult.Scheduled(at("2026-11-12T08:00")), result)
        assertEquals(at("2026-11-12T08:00"), scheduler.scheduledAt)
    }

    @Test
    fun `disabled alarm is cancelled`() = runTest {
        scheduler.schedule(at("2026-11-12T08:00"))

        val result = sync(enabled.copy(enabled = false), now = "2026-11-12T06:00").sync()

        assertEquals(SyncResult.Disabled, result)
        assertNull(scheduler.scheduledAt)
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

        assertEquals(SyncResult.Scheduled(at("2026-11-13T08:00")), result)
    }
}
