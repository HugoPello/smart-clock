package com.hugopello.smartclock.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextOccurrenceTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), madrid)

    @Test
    fun `later today stays today`() {
        val next = nextOccurrence(LocalTime.of(7, 0), now = at("2026-11-12T06:00"))

        assertEquals(at("2026-11-12T07:00"), next)
    }

    @Test
    fun `already passed today moves to tomorrow`() {
        val next = nextOccurrence(LocalTime.of(7, 0), now = at("2026-11-12T22:00"))

        assertEquals(at("2026-11-13T07:00"), next)
    }

    @Test
    fun `exactly now moves to tomorrow`() {
        // Otherwise an alarm rescheduling itself while ringing would ring again immediately.
        val next = nextOccurrence(LocalTime.of(7, 0), now = at("2026-11-12T07:00"))

        assertEquals(at("2026-11-13T07:00"), next)
    }

    @Test
    fun `crosses month and year boundaries`() {
        val next = nextOccurrence(LocalTime.of(7, 0), now = at("2026-12-31T23:00"))

        assertEquals(at("2027-01-01T07:00"), next)
    }

    @Test
    fun `time inside the spring forward gap rings right after the jump`() {
        // 29 March 2026 in Madrid: 02:00 → 03:00, so 02:30 doesn't exist that day.
        val next = nextOccurrence(LocalTime.of(2, 30), now = at("2026-03-29T01:00"))

        assertEquals(LocalTime.of(3, 30), next.toLocalTime())
    }

    @Test
    fun `keeps wall clock time across a daylight saving change`() {
        // 25 October 2026: clocks go back 03:00 → 02:00. 07:00 is still 07:00 on the wall.
        val next = nextOccurrence(LocalTime.of(7, 0), now = at("2026-10-24T22:00"))

        assertEquals(at("2026-10-25T07:00"), next)
    }
}
