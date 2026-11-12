package com.hugopello.smartclock.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalTime

class AlarmCalculatorTest {

    // Base scenario: arrive 9:00, 30 min to get ready, usual trip 30 min → original alarm 8:00.
    private fun input(
        todayTravelMin: Long?,
        currentAlarm: LocalTime? = null,
        arrival: LocalTime = LocalTime.of(9, 0),
    ) = AlarmInput(
        arrival = arrival,
        preparation = Duration.ofMinutes(30),
        usualTravel = Duration.ofMinutes(30),
        todayTravel = todayTravelMin?.let { Duration.ofMinutes(it) },
        currentAlarm = currentAlarm,
    )

    private fun assertDecision(expected: LocalTime, reason: DecisionReason, actual: AlarmDecision) {
        assertEquals(AlarmDecision(expected, reason), actual)
    }

    // --- Adjusting to traffic ---

    @Test
    fun `trip 10 min longer moves alarm 10 min earlier`() {
        assertDecision(LocalTime.of(7, 50), DecisionReason.ADJUSTED, calculateAlarm(input(40)))
    }

    @Test
    fun `trip 10 min shorter moves alarm 10 min later`() {
        assertDecision(LocalTime.of(8, 10), DecisionReason.ADJUSTED, calculateAlarm(input(20)))
    }

    @Test
    fun `same travel time as usual keeps original time`() {
        assertDecision(LocalTime.of(8, 0), DecisionReason.BELOW_THRESHOLD, calculateAlarm(input(30)))
    }

    // --- Limits ---

    @Test
    fun `trip 60 min longer is capped at 30 min earlier`() {
        assertDecision(LocalTime.of(7, 30), DecisionReason.CAPPED_EARLIER, calculateAlarm(input(90)))
    }

    @Test
    fun `shift exactly at earlier limit is not reported as capped`() {
        assertDecision(LocalTime.of(7, 30), DecisionReason.ADJUSTED, calculateAlarm(input(60)))
    }

    @Test
    fun `trip 25 min shorter is capped at 15 min later`() {
        assertDecision(LocalTime.of(8, 15), DecisionReason.CAPPED_LATER, calculateAlarm(input(5)))
    }

    @Test
    fun `custom user limits are respected`() {
        val strict = AlarmLimits(maxEarlier = Duration.ofMinutes(10))
        assertDecision(LocalTime.of(7, 50), DecisionReason.CAPPED_EARLIER, calculateAlarm(input(60), strict))
    }

    // --- Network failure ---

    @Test
    fun `no traffic data keeps original time`() {
        assertDecision(LocalTime.of(8, 0), DecisionReason.NO_TRAFFIC_DATA, calculateAlarm(input(null)))
    }

    @Test
    fun `no traffic data on second query keeps already adjusted alarm`() {
        val decision = calculateAlarm(input(null, currentAlarm = LocalTime.of(7, 45)))
        assertDecision(LocalTime.of(7, 45), DecisionReason.NO_TRAFFIC_DATA, decision)
    }

    // --- Anti-oscillation ---

    @Test
    fun `change smaller than threshold keeps current alarm`() {
        // First query put it at 7:50; now traffic says 7:48 (2 min) → don't move.
        val decision = calculateAlarm(input(42, currentAlarm = LocalTime.of(7, 50)))
        assertDecision(LocalTime.of(7, 50), DecisionReason.BELOW_THRESHOLD, decision)
    }

    @Test
    fun `change exactly equal to threshold moves the alarm`() {
        val decision = calculateAlarm(input(43, currentAlarm = LocalTime.of(7, 50)))
        assertDecision(LocalTime.of(7, 47), DecisionReason.ADJUSTED, decision)
    }

    @Test
    fun `second query can move alarm back later`() {
        // Traffic cleared up: 7:50 → 8:00.
        val decision = calculateAlarm(input(30, currentAlarm = LocalTime.of(7, 50)))
        assertDecision(LocalTime.of(8, 0), DecisionReason.ADJUSTED, decision)
    }

    // --- Midnight ---

    @Test
    fun `alarm before midnight wraps around`() {
        // Arrive 00:30 → original 23:30 the day before; +10 min trip → 23:20.
        val decision = calculateAlarm(input(40, arrival = LocalTime.of(0, 30)))
        assertDecision(LocalTime.of(23, 20), DecisionReason.ADJUSTED, decision)
    }

    @Test
    fun `threshold works across midnight`() {
        // Current 23:59, candidate 00:01 → 2 min apart, not 23h58.
        val decision = calculateAlarm(
            input(28, currentAlarm = LocalTime.of(23, 59), arrival = LocalTime.of(0, 59)),
        )
        assertDecision(LocalTime.of(23, 59), DecisionReason.BELOW_THRESHOLD, decision)
    }
}
