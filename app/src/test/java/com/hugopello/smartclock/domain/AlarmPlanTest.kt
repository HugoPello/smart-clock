package com.hugopello.smartclock.domain

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

class AlarmPlanTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), zone)

    // 8:00 − 30 − 30 → original alarm 07:00, check at 07:00 − 30 − 15 = 06:15
    private val settings = AlarmSettings(
        arrival = LocalTime.of(8, 0),
        preparation = Duration.ofMinutes(30),
        usualTravel = Duration.ofMinutes(30),
        destination = "Office",
        enabled = true,
        origin = "Home",
    )

    private fun adjustment(date: String, shiftMinutes: Long) = TrafficAdjustment(
        LocalDate.parse(date),
        Duration.ofMinutes(shiftMinutes),
        Duration.ofMinutes(30 - shiftMinutes),
        DecisionReason.ADJUSTED,
        Instant.EPOCH,
    )

    @Test
    fun `not checked yet - original time and a check 45 min before`() {
        val plan = planAlarm(settings, adjustment = null, from = at("2026-11-12T05:00"))

        assertEquals(at("2026-11-12T07:00"), plan.ring)
        assertEquals(at("2026-11-12T06:15"), plan.trafficCheck)
    }

    @Test
    fun `checked - adjustment moves the ring and there is nothing left to check`() {
        val plan = planAlarm(settings, adjustment("2026-11-12", -20), from = at("2026-11-12T06:16"))

        assertEquals(at("2026-11-12T06:40"), plan.ring)
        assertNull(plan.trafficCheck)
    }

    @Test
    fun `adjustment for another day is ignored`() {
        val plan = planAlarm(settings, adjustment("2026-11-11", -20), from = at("2026-11-12T05:00"))

        assertEquals(at("2026-11-12T07:00"), plan.ring)
    }

    @Test
    fun `alarm moved later is kept after the original time has passed`() {
        // Original 07:00 moved to 07:15; the app is opened at 07:05: today's alarm is still pending.
        val plan = planAlarm(settings, adjustment("2026-11-12", 15), from = at("2026-11-12T07:05"))

        assertEquals(at("2026-11-12T07:15"), plan.ring)
    }

    @Test
    fun `alarm moved earlier that already rang is not repeated`() {
        // Rang at 06:40 (moved −20). At 06:45 the original 07:00 is still ahead, but that day is done.
        val plan = planAlarm(settings, adjustment("2026-11-12", -20), from = at("2026-11-12T06:45"))

        assertEquals(at("2026-11-13T07:00"), plan.ring)
        assertEquals(at("2026-11-13T06:15"), plan.trafficCheck)
    }

    @Test
    fun `no adjustment and original time passed - tomorrow`() {
        val plan = planAlarm(settings, adjustment = null, from = at("2026-11-12T07:05"))

        assertEquals(at("2026-11-13T07:00"), plan.ring)
    }
}
